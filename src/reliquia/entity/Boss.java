package reliquia.entity;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import reliquia.core.GamePanel;

/**
 * CLASE BOSS — La Bestia, jefe final.
 *
 * HERENCIA en acción: Boss ES un Enemy (cabe en la lista gp.enemigos y
 * el resto del juego lo trata igual), pero SOBREESCRIBE update(), draw()
 * y alMorir() con comportamiento propio. Eso es polimorfismo.
 *
 * Su patrón reconocible es un ciclo de TRES FASES (otra máquina de
 * estados, en miniatura):
 *
 *   ESPERA (70 frames)  → se queda quieto; al final TIEMBLA: ese
 *                          temblor es el "aviso" que el jugador aprende
 *                          a leer para esquivar.
 *   EMBESTIDA           → carga en línea recta hacia el jugador hasta
 *                          estrellarse contra un muro.
 *   ATURDIDO (50 frames)→ pausa tras el choque: la ventana ideal
 *                          para acercarse y golpear.
 *
 * Lección de diseño: un buen jefe no es difícil por azar, sino legible.
 * El jugador debe poder PREDECIR el ataque y planear su respuesta.
 */
public class Boss extends Enemy {

    public static final int FASE_ESPERA = 0;
    public static final int FASE_EMBESTIDA = 1;
    public static final int FASE_ATURDIDO = 2;

    private int fase = FASE_ESPERA;
    private int contadorFase = 0;

    // La Bestia ocupa 2x2 tiles (96 px): impone solo por tamaño.
    public static final int TAM = GamePanel.TAMANO_TILE * 2;

    public Boss(GamePanel gp, int columna, int fila, int pantalla) {
        super(gp, columna, fila, pantalla, Enemy.JEFE);

        vidaMaxima = 8;
        vida = 8;
        velocidad = 2;
        areaSolida = new Rectangle(8, 8, TAM - 16, TAM - 16);
        cargarSpriteBestia();
    }

    @Override
    public void update() {

        contadorFase++;

        switch (fase) {

            case FASE_ESPERA:
                if (contadorFase >= 70) {
                    direccion = direccionHaciaJugador();  // apunta y...
                    velocidad = 6;                        // ...¡carga!
                    fase = FASE_EMBESTIDA;
                    contadorFase = 0;
                }
                break;

            case FASE_EMBESTIDA:
                colisionDetectada = false;
                gp.colisiones.revisarTile(this);
                gp.colisiones.revisarObjetos(this);
                if (tocaBordeJefe()) colisionDetectada = true;

                if (colisionDetectada) {
                    // Se estrella: queda aturdido y vulnerable.
                    velocidad = 2;
                    fase = FASE_ATURDIDO;
                    contadorFase = 0;
                } else {
                    moverSegunDireccion();
                }
                break;

            case FASE_ATURDIDO:
                if (contadorFase >= 50) {
                    fase = FASE_ESPERA;
                    contadorFase = 0;
                }
                break;
        }

        actualizarInvencibilidad();
    }

    /** Elige la dirección cardinal dominante hacia el héroe:
     *  comparamos las distancias horizontal y vertical y gana la mayor. */
    private String direccionHaciaJugador() {
        int dx = gp.jugador.x - x;
        int dy = gp.jugador.y - y;
        if (Math.abs(dx) > Math.abs(dy)) {
            return (dx > 0) ? "derecha" : "izquierda";
        }
        return (dy > 0) ? "abajo" : "arriba";
    }

    /** Versión del chequeo de borde adaptada a su tamaño doble. */
    private boolean tocaBordeJefe() {
        switch (direccion) {
            case "arriba":    return y - velocidad < 0;
            case "abajo":     return y + TAM + velocidad > GamePanel.ALTO;
            case "izquierda": return x - velocidad < 0;
            default:          return x + TAM + velocidad > GamePanel.ANCHO;
        }
    }

    /** Al caer, suelta la Reliquia: recogerla dispara la VICTORIA
     *  (eso ya lo gestiona Player.recogerObjeto). */
    @Override
    public void alMorir() {
        int t = GamePanel.TAMANO_TILE;
        int columna = (x + TAM / 2) / t;
        int fila = (y + TAM / 2) / t;
        gp.objetos.add(new GameObject(gp, "reliquia",
                GameObject.RELIQUIA, columna, fila, pantalla));
        gp.ui.mostrarMensaje("¡La Bestia cae! Algo brilla en el suelo...");
    }

    /** Dibujado propio: tamaño doble, temblor de aviso y barra de vida. */
    @Override
    public void draw(Graphics2D g2) {

        if (pantalla != gp.mapa.pantallaActual) return;
        if (invencible && (contadorInvencible / 4) % 2 == 0) return;

        // Temblor en el tramo final de la espera: el "telegrafiado".
        int sacudida = 0;
        if (fase == FASE_ESPERA && contadorFase > 40) {
            sacudida = (contadorFase % 4 < 2) ? -2 : 2;
        }

        g2.drawImage(spriteAbajo, x + sacudida, y, TAM, TAM, null);
        dibujarBarraDeVida(g2);
    }

    /** Barra de vida clásica de jefe, en la parte superior. */
    private void dibujarBarraDeVida(Graphics2D g2) {
        int ancho = 300;
        int bx = GamePanel.ANCHO / 2 - ancho / 2;
        int by = 52;

        g2.setColor(UI.NEGRO);
        g2.fillRect(bx - 2, by - 2, ancho + 4, 16);
        g2.setColor(UI.BLANCO);
        g2.drawRect(bx - 2, by - 2, ancho + 3, 15);

        // El relleno es proporcional a la vida restante.
        int relleno = (int) (ancho * (vida / (double) vidaMaxima));
        g2.fillRect(bx, by, relleno, 12);
    }

    private void cargarSpriteBestia() {
        String[] patron = {
            "..33........33..",
            "..33........33..",
            "..333333333333..",
            ".33222222222233.",
            ".32202222220223.",
            ".32222222222223.",
            ".32233333333223.",
            ".32230303032223.",
            "..333333333333..",
            "...3222222223...",
            "..322222222223..",
            "..322222222223..",
            "..332222222233..",
            "...3333333333...",
            "..33........33..",
            "................"
        };
        spriteAbajo = crearSprite(patron);
        spriteArriba = spriteAbajo;
        spriteIzquierda = spriteAbajo;
        spriteDerecha = spriteAbajo;
    }
}