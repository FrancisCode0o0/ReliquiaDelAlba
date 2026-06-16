package reliquia.entity;
import reliquia.core.GamePanel;
import reliquia.object.GameObject;

/**
 * CLASE ENEMY — Enemigos con patrones simples.
 *
 * Hereda de Entity todo lo común y añade un "cerebro" mínimo basado en
 * contadores: cada N frames (o al chocar) el enemigo decide una nueva
 * dirección. No hay inteligencia real: solo azar + rebote. Es sorprendente
 * lo vivo que parece algo tan simple.
 *
 * Dos patrones reconocibles:
 *  - BABOSA: lenta, deambula al azar. Al chocar, dirección aleatoria.
 *  - MURCIÉLAGO: rápido, vuela en línea recta y REBOTA al chocar
 *    (dirección opuesta). Cambia de rumbo de vez en cuando.
 *
 * El tipo JEFE existe solo como "hueco" para que la clase Boss herede
 * de aquí sin cargar sprites que no usa.
 */
public class Enemy extends Entity {

    public static final int BABOSA = 0;
    public static final int MURCIELAGO = 1;
    public static final int JEFE = 2;

    public int tipo;
    protected int contadorPatron = 0;  // frames desde la última decisión

    // Para dar un ID único a cada corazón que sueltan al morir.
    private static int contadorBotin = 0;

    public Enemy(GamePanel gp, int columna, int fila, int pantalla, int tipo) {
        super(gp);
        this.tipo = tipo;
        this.pantalla = pantalla;
        colocarEnTile(columna, fila);

        switch (tipo) {
            case BABOSA:
                velocidad = 1;
                vidaMaxima = 2; vida = 2;     // aguanta dos espadazos
                cargarSpritesBabosa();
                break;
            case MURCIELAGO:
                velocidad = 3;
                vidaMaxima = 1; vida = 1;     // frágil pero rápido
                cargarSpritesMurcielago();
                break;
            case JEFE:
                break;  // Boss configura lo suyo en su propio constructor
        }
    }

    /** El "cerebro" del enemigo: decidir → comprobar colisión → moverse. */
    @Override
    public void update() {

        contadorPatron++;

        // Decisión periódica según el patrón de cada tipo.
        if (tipo == BABOSA && contadorPatron >= 90) {       // cada 1,5 s
            direccion = direccionAleatoria();
            contadorPatron = 0;
        }
        if (tipo == MURCIELAGO && contadorPatron >= 150) {  // cada 2,5 s
            direccion = direccionAleatoria();
            contadorPatron = 0;
        }

        // Mismo protocolo que el jugador: preguntar ANTES de moverse.
        colisionDetectada = false;
        gp.colisiones.revisarTile(this);
        gp.colisiones.revisarObjetos(this);   // cofres y puertas también frenan
        if (tocaBordePantalla()) {
            colisionDetectada = true;         // que no se escapen por las salidas
        }

        if (colisionDetectada) {
            // Reacción al choque: la babosa improvisa, el murciélago rebota.
            direccion = (tipo == MURCIELAGO) ? direccionOpuesta()
                                             : direccionAleatoria();
        } else {
            moverSegunDireccion();
        }

        animar();
        actualizarInvencibilidad();
    }

    /** ¿El próximo paso lo sacaría de la pantalla? */
    protected boolean tocaBordePantalla() {
        switch (direccion) {
            case "arriba":    return y - velocidad < 0;
            case "abajo":     return y + velocidad > GamePanel.ALTO - GamePanel.TAMANO_TILE;
            case "izquierda": return x - velocidad < 0;
            default:          return x + velocidad > GamePanel.ANCHO - GamePanel.TAMANO_TILE;
        }
    }

    protected String direccionAleatoria() {
        String[] opciones = { "arriba", "abajo", "izquierda", "derecha" };
        return opciones[(int) (Math.random() * 4)];
    }

    protected String direccionOpuesta() {
        switch (direccion) {
            case "arriba":    return "abajo";
            case "abajo":     return "arriba";
            case "izquierda": return "derecha";
            default:          return "izquierda";
        }
    }

    /**
     * Al morir, 50% de probabilidad de soltar un corazón.
     * Nota de diseño: estos botines NO se recrean al cargar partida
     * (colocarEntidades no los conoce). Es una simplificación asumida.
     */
    public void alMorir() {
        if (Math.random() < 0.5) {
            int t = GamePanel.TAMANO_TILE;
            int columna = (x + t / 2) / t;
            int fila = (y + t / 2) / t;
            gp.objetos.add(new GameObject(gp, "botin_" + (contadorBotin++),
                    GameObject.CORAZON, columna, fila, pantalla));
        }
    }

    // --- Sprites: la babosa es igual mirando a cualquier lado ---
    private void cargarSpritesBabosa() {
        String[] patron = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "......2222......",
            ".....222222.....",
            "....22222222....",
            "...2230230222...",
            "...2233233222...",
            "..222222222222..",
            ".22222222222222.",
            ".23222222222232.",
            ".22222222222222.",
            "..222222222222..",
            "................"
        };
        spriteAbajo = crearSprite(patron);
        spriteArriba = spriteAbajo;       // misma imagen para las 4 direcciones
        spriteIzquierda = spriteAbajo;
        spriteDerecha = spriteAbajo;
    }

    private void cargarSpritesMurcielago() {
        String[] patron = {
            "................",
            "................",
            "................",
            "..3..........3..",
            "..33........33..",
            "..333..33..333..",
            "..333333333333..",
            ".33333333333333.",
            ".33303333033333.",
            "..333333333333..",
            "...3333333333...",
            "....33....33....",
            "...3........3...",
            "................",
            "................",
            "................"
        };
        spriteDerecha = crearSprite(patron);
        spriteIzquierda = crearEspejo(spriteDerecha);
        spriteAbajo = spriteDerecha;      // vertical: reutilizamos el mismo
        spriteArriba = spriteDerecha;
    }
}