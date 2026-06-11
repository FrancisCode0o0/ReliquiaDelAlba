/**
 * CLASE NPC — Personajes no jugables.
 *
 * Los aldeanos son las entidades más simples: no se mueven, no pelean,
 * no tienen colisiones que calcular. Solo existen para dar vida y para
 * HABLAR. Su diálogo es un array de líneas que reciben en el
 * constructor mediante VARARGS (String... lineas): esa sintaxis permite
 * pasar 1, 2 o 20 frases sin crear el array a mano.
 *
 * hablar() no muestra texto por sí mismo: delega en la UI y cambia el
 * estado del juego a DIALOGO. Cada clase a lo suyo (separación de
 * responsabilidades).
 */
public class NPC extends Entity {

    private String[] lineas;

    public NPC(GamePanel gp, int columna, int fila, int pantalla, String... lineas) {
        super(gp);
        this.pantalla = pantalla;
        this.lineas = lineas;
        colocarEnTile(columna, fila);
        velocidad = 0;        // los aldeanos no caminan
        cargarSprite();
    }

    /** Sin movimiento, pero con el balanceo de Entity: un NPC que
     *  "respira" parece vivo; uno congelado parece un mueble. */
    @Override
    public void update() {
        animar();
    }

    /** Entrega sus líneas a la UI, que abre la ventana de diálogo
     *  y congela el mundo (estado DIALOGO). */
    public void hablar() {
        gp.ui.iniciarDialogo(lineas);
    }

    private void cargarSprite() {
        // Aldeano con capucha: silueta distinta a la del héroe.
        String[] patron = {
            "................",
            ".....222222.....",
            "....22222222....",
            "...2222222222...",
            "....30000003....",
            "....30300303....",
            "....30000003....",
            ".....300003.....",
            "....31111113....",
            "...3111111113...",
            "...3111111113...",
            "...3111111113...",
            "....31111113....",
            "....311..113....",
            "....333..333....",
            "................"
        };
        spriteAbajo = crearSprite(patron);
        spriteArriba = spriteAbajo;
        spriteIzquierda = spriteAbajo;
        spriteDerecha = spriteAbajo;
    }
}