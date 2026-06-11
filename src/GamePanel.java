import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

/**
 * CLASE GAMEPANEL — El corazón del juego.
 *
 * Aquí viven tres cosas fundamentales:
 *
 *  1. EL BUCLE PRINCIPAL (método run): un juego no es más que un bucle
 *     infinito que repite "actualizar lógica → dibujar" unas 60 veces
 *     por segundo. Cada repetición se llama "frame" (fotograma).
 *
 *  2. EL SISTEMA DE ESTADOS: el juego se comporta distinto según esté
 *     en el título, jugando, en un diálogo, etc. Lo modelamos con una
 *     variable 'estado' y constantes. Es una "máquina de estados": la
 *     forma más simple y robusta de organizar un juego.
 *
 *  3. EL RENDERIZADO (método paintComponent): el orden en que dibujamos
 *     importa. Primero el suelo, encima los objetos, encima los seres,
 *     y al final la interfaz. Como capas de papel.
 */
public class GamePanel extends JPanel implements Runnable {

    // ------------------------------------------------------------------
    // MEDIDAS DE LA PANTALLA
    // Los sprites originales miden 16x16 píxeles (como en las consolas
    // clásicas), pero los dibujamos escalados x3 para verlos bien hoy.
    // ------------------------------------------------------------------
    public static final int TILE_BASE = 16;                       // tamaño "real" del sprite
    public static final int ESCALA = 3;                           // factor de ampliación
    public static final int TAMANO_TILE = TILE_BASE * ESCALA;     // 48 px en pantalla
    public static final int MAX_COLUMNAS = 16;                    // tiles a lo ancho
    public static final int MAX_FILAS = 12;                       // tiles a lo alto
    public static final int ANCHO = TAMANO_TILE * MAX_COLUMNAS;   // 768 px
    public static final int ALTO = TAMANO_TILE * MAX_FILAS;       // 576 px

    public static final int FPS = 60;  // fotogramas por segundo objetivo

    // ------------------------------------------------------------------
    // SISTEMA DE ESTADOS
    // En cada frame, el juego mira 'estado' para decidir qué actualizar
    // y qué dibujar. Cambiar de pantalla (título → juego → derrota) es
    // simplemente cambiar el valor de esta variable.
    // ------------------------------------------------------------------
    public static final int ESTADO_TITULO = 0;
    public static final int ESTADO_JUEGO = 1;
    public static final int ESTADO_DIALOGO = 2;
    public static final int ESTADO_INVENTARIO = 3;
    public static final int ESTADO_GAME_OVER = 4;
    public static final int ESTADO_VICTORIA = 5;

    public int estado = ESTADO_TITULO;

    // ------------------------------------------------------------------
    // SISTEMAS DEL JUEGO
    // El orden de declaración importa: Java los crea de arriba a abajo,
    // y 'jugador' necesita que 'teclado' ya exista.
    // ------------------------------------------------------------------
    public KeyHandler teclado = new KeyHandler(this);
    Thread hiloJuego;                                   // el hilo del bucle principal
    public GameMap mapa = new GameMap(this);
    public CollisionChecker colisiones = new CollisionChecker(this);
    public UI ui = new UI(this);
    public SaveManager guardado = new SaveManager(this);
    public Player jugador = new Player(this, teclado);

    // Listas de seres y objetos del mundo. Cada uno guarda en qué
    // pantalla vive; solo actualizamos y dibujamos los de la pantalla actual.
    public ArrayList<NPC> npcs = new ArrayList<>();
    public ArrayList<Enemy> enemigos = new ArrayList<>();
    public ArrayList<GameObject> objetos = new ArrayList<>();

    // IDs de objetos ya recogidos/abiertos. Sirve para el guardado:
    // al cargar la partida, sabemos qué cofres no deben reaparecer.
    public ArrayList<String> objetosRecogidos = new ArrayList<>();

    public GamePanel() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(Color.BLACK);

        // Doble búfer: Swing dibuja primero en una imagen oculta y luego
        // la muestra de golpe. Sin esto, veríamos parpadeos.
        setDoubleBuffered(true);

        // Conectamos el oyente de teclado y pedimos el "foco":
        // solo el componente con foco recibe los eventos de teclas.
        addKeyListener(teclado);
        setFocusable(true);
    }

    /** Arranca el hilo del juego. Lo llama Main una sola vez. */
    public void iniciar() {
        hiloJuego = new Thread(this);
        hiloJuego.start();   // esto ejecuta run() en paralelo
    }

    /** Prepara una partida desde cero. */
    public void nuevaPartida() {
        jugador.valoresIniciales();
        objetosRecogidos.clear();
        colocarEntidades();
        mapa.pantallaActual = 0;
        estado = ESTADO_JUEGO;
    }

    // ------------------------------------------------------------------
    // EL BUCLE PRINCIPAL DEL JUEGO
    //
    // Queremos 60 actualizaciones por segundo, ni más ni menos. Para eso
    // usamos un "acumulador delta":
    //
    //   - 'intervalo' es cuánto debe durar un frame en nanosegundos
    //     (1 segundo / 60 ≈ 16,6 millones de ns).
    //   - En cada vuelta medimos cuánto tiempo real pasó y lo sumamos
    //     a 'delta' en unidades de frame.
    //   - Cuando delta llega a 1, ha pasado el tiempo de un frame:
    //     actualizamos, dibujamos y restamos 1.
    //
    // Ventaja: si el ordenador se atasca un instante, delta acumula más
    // de 1 y el juego recupera el ritmo sin volverse lento.
    // ------------------------------------------------------------------
    @Override
    public void run() {

        double intervalo = 1_000_000_000.0 / FPS;  // duración de un frame en ns
        double delta = 0;
        long tiempoAnterior = System.nanoTime();

        while (hiloJuego != null) {

            long tiempoActual = System.nanoTime();
            delta += (tiempoActual - tiempoAnterior) / intervalo;
            tiempoAnterior = tiempoActual;

            if (delta >= 1) {
                actualizar();   // 1. mover el mundo
                repaint();      // 2. pedir a Swing que llame a paintComponent
                delta--;
            }
        }
    }

    /**
     * ACTUALIZACIÓN DE LA LÓGICA (se ejecuta 60 veces por segundo).
     * Fíjate en la máquina de estados: el mundo SOLO se mueve durante
     * ESTADO_JUEGO. En un diálogo o en el inventario, todo queda
     * congelado. Esa es la gracia del sistema de estados.
     */
    public void actualizar() {

        if (estado != ESTADO_JUEGO) {
            return;  // mundo en pausa en cualquier otro estado
        }

        jugador.update();

        // Recorremos hacia atrás porque podemos BORRAR enemigos muertos:
        // al borrar el índice i, los siguientes se desplazan, y recorrer
        // hacia atrás evita saltarnos elementos.
        for (int i = enemigos.size() - 1; i >= 0; i--) {
            Enemy enemigo = enemigos.get(i);
            if (enemigo.pantalla == mapa.pantallaActual) {
                enemigo.update();
                if (enemigo.vida <= 0) {
                    enemigo.alMorir();       // puede soltar un corazón
                    enemigos.remove(i);
                }
            }
        }

        for (NPC npc : npcs) {
            if (npc.pantalla == mapa.pantallaActual) {
                npc.update();
            }
        }
    }

    // ------------------------------------------------------------------
    // EL SISTEMA DE RENDERIZADO
    //
    // Swing llama a paintComponent cada vez que hacemos repaint().
    // Dibujamos por CAPAS, de fondo a frente:
    //   1. tiles del mapa  2. objetos  3. NPCs  4. enemigos
    //   5. jugador         6. interfaz (corazones, diálogos, menús)
    // Si invirtiéramos el orden, el suelo taparía al héroe.
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  // limpia el frame anterior

        Graphics2D g2 = (Graphics2D) g;

        // "Vecino más cercano": al escalar sprites, NO suavizar.
        // Queremos píxeles cuadrados y nítidos, no borrosos.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        if (estado == ESTADO_TITULO) {
            ui.dibujar(g2);   // el título no necesita el mundo detrás
            g2.dispose();
            return;
        }

        mapa.dibujar(g2);

        for (GameObject objeto : objetos) objeto.dibujar(g2);
        for (NPC npc : npcs)              npc.draw(g2);
        for (Enemy enemigo : enemigos)    enemigo.draw(g2);

        jugador.draw(g2);

        ui.dibujar(g2);   // la interfaz siempre va encima de todo

        g2.dispose();     // liberar recursos gráficos del frame
    }

    // ------------------------------------------------------------------
    // POBLAR EL MUNDO
    // Cada entidad recibe (columna, fila, pantalla). Los objetos llevan
    // además un ID único de texto: es lo que anotamos en
    // 'objetosRecogidos' para que el guardado sepa qué ya no existe.
    // ------------------------------------------------------------------
    public void colocarEntidades() {

        npcs.clear();
        enemigos.clear();
        objetos.clear();

        // ===== PANTALLA 0: LA ALDEA =====
        npcs.add(new NPC(this, 5, 5, 0,
                "¡Salve, héroe! Una bestia ha despertado en la cueva del norte.",
                "El bosque, al este, guarda una llave en uno de sus cofres.",
                "Y recuerda: quien guarda con G, siempre regresa."));

        npcs.add(new NPC(this, 10, 8, 0,
                "Mi abuela decía: tres corazones no bastan ante la bestia.",
                "Las pociones del inventario curan dos corazones. Pulsa I."));

        GameObject cartelAldea = new GameObject(this, "cartel_aldea",
                GameObject.CARTEL, 8, 4, 0);
        cartelAldea.texto = "ALDEA DEL ALBA. Al este: el bosque.";
        objetos.add(cartelAldea);

        GameObject cofreAldea = new GameObject(this, "cofre_aldea",
                GameObject.COFRE, 2, 9, 0);
        cofreAldea.contenido = "Poción";
        objetos.add(cofreAldea);

        // ===== PANTALLA 1: EL BOSQUE =====
        enemigos.add(new Enemy(this, 4, 4, 1, Enemy.BABOSA));
        enemigos.add(new Enemy(this, 11, 8, 1, Enemy.BABOSA));

        objetos.add(new GameObject(this, "moneda_b1", GameObject.MONEDA, 3, 9, 1));
        objetos.add(new GameObject(this, "moneda_b2", GameObject.MONEDA, 12, 3, 1));

        GameObject cofreBosque = new GameObject(this, "cofre_bosque",
                GameObject.COFRE, 13, 9, 1);
        cofreBosque.contenido = "Llave";
        objetos.add(cofreBosque);

        // ===== PANTALLA 2: EL LAGO =====
        enemigos.add(new Enemy(this, 3, 4, 2, Enemy.MURCIELAGO));
        enemigos.add(new Enemy(this, 12, 7, 2, Enemy.MURCIELAGO));

        objetos.add(new GameObject(this, "corazon_lago", GameObject.CORAZON, 13, 2, 2));
        objetos.add(new GameObject(this, "moneda_lago", GameObject.MONEDA, 2, 8, 2));

        GameObject cartelLago = new GameObject(this, "cartel_lago",
                GameObject.CARTEL, 5, 2, 2);
        cartelLago.texto = "Más allá duerme la bestia. Solo una llave abre su morada.";
        objetos.add(cartelLago);

        // Puerta cerrada que bloquea la entrada norte a la mazmorra.
        objetos.add(new GameObject(this, "puerta_mazmorra",
                GameObject.PUERTA, 7, 0, 2));

        // ===== PANTALLA 3: LA MAZMORRA =====
        enemigos.add(new Enemy(this, 5, 5, 3, Enemy.BABOSA));
        enemigos.add(new Enemy(this, 10, 7, 3, Enemy.BABOSA));
        enemigos.add(new Enemy(this, 4, 8, 3, Enemy.MURCIELAGO));

        // Acertijo: la palanca abre la puerta del jefe (ver Player.interactuar).
        objetos.add(new GameObject(this, "palanca_jefe",
                GameObject.PALANCA, 2, 2, 3));

        GameObject cartelMazmorra = new GameObject(this, "cartel_mazmorra",
                GameObject.CARTEL, 4, 2, 3);
        cartelMazmorra.texto = "La palanca despierta puertas dormidas.";
        objetos.add(cartelMazmorra);

        GameObject cofreMazmorra = new GameObject(this, "cofre_mazmorra",
                GameObject.COFRE, 13, 2, 3);
        cofreMazmorra.contenido = "Poción";
        objetos.add(cofreMazmorra);

        objetos.add(new GameObject(this, "moneda_maz", GameObject.MONEDA, 12, 9, 3));
        objetos.add(new GameObject(this, "puerta_jefe", GameObject.PUERTA, 8, 0, 3));

        // ===== PANTALLA 4: SALA DEL JEFE =====
        // Boss HEREDA de Enemy, así que cabe en la lista de enemigos.
        // Esto se llama POLIMORFISMO: tratamos al jefe como un enemigo más.
        enemigos.add(new Boss(this, 7, 3, 4));
    }

    /**
     * Tras cargar una partida, aplica el estado guardado: cofres ya
     * abiertos, puertas ya destruidas, objetos ya recogidos.
     * Recorremos una COPIA de la lista para poder borrar de la original
     * sin provocar errores de modificación concurrente.
     */
    public void aplicarObjetosRecogidos() {
        for (GameObject objeto : new ArrayList<>(objetos)) {
            if (objetosRecogidos.contains(objeto.id)) {
                if (objeto.tipo == GameObject.COFRE) {
                    objeto.abierto = true;
                } else if (objeto.tipo == GameObject.PALANCA) {
                    objeto.activada = true;
                    eliminarObjetoPorId("puerta_jefe");  // efecto de la palanca
                } else {
                    objetos.remove(objeto);
                }
            }
        }
    }

    /** Busca un objeto por su ID y lo elimina del mundo. */
    public void eliminarObjetoPorId(String id) {
        for (int i = 0; i < objetos.size(); i++) {
            if (objetos.get(i).id.equals(id)) {
                objetos.remove(i);
                return;
            }
        }
    }
}