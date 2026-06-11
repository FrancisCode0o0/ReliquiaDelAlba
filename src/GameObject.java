import java.awt.Graphics2D;

/**
 * CLASE GAMEOBJECT — Todo lo que puebla el escenario sin estar vivo.
 *
 * Un solo molde para nueve cosas distintas (cofres, monedas, puertas...).
 * En lugar de nueve clases, usamos un campo 'tipo' + dos propiedades
 * que definen cómo se comporta el mundo con él:
 *
 *   solido    → bloquea el paso (lo respeta CollisionChecker)
 *   recogible → se obtiene al pisarlo (lo gestiona Player)
 *
 * Decisión de diseño: las ENTIDADES se dibujan con matrices de sprites,
 * pero los objetos se dibujan con PRIMITIVAS de Graphics2D (fillRect,
 * fillOval...). Así veis las dos técnicas en el mismo proyecto. Ambas
 * son "gráficos generados por código".
 *
 * El campo 'id' es un nombre único ("cofre_bosque", "moneda_b1"...).
 * Es la pieza clave del guardado: en el archivo de partida apuntamos
 * los IDs ya recogidos, y al cargar sabemos exactamente qué eliminar.
 */
public class GameObject {

    // Tipos posibles. Constantes con nombre > números mágicos sueltos.
    public static final int CARTEL = 0;
    public static final int COFRE = 1;
    public static final int PUERTA = 2;
    public static final int PALANCA = 3;
    public static final int MONEDA = 4;
    public static final int CORAZON = 5;
    public static final int LLAVE = 6;
    public static final int POCION = 7;
    public static final int RELIQUIA = 8;

    GamePanel gp;
    public String id;
    public int tipo;
    public int x, y;
    public int pantalla;

    public boolean solido = false;
    public boolean recogible = false;

    public String texto = "";       // lo que dice un cartel
    public String contenido = "";   // lo que esconde un cofre
    public boolean abierto = false;     // estado del cofre
    public boolean activada = false;    // estado de la palanca

    public GameObject(GamePanel gp, String id, int tipo,
                      int columna, int fila, int pantalla) {
        this.gp = gp;
        this.id = id;
        this.tipo = tipo;
        this.pantalla = pantalla;
        x = columna * GamePanel.TAMANO_TILE;
        y = fila * GamePanel.TAMANO_TILE;

        switch (tipo) {
            case MONEDA: case CORAZON: case LLAVE: case POCION: case RELIQUIA:
                recogible = true;   // se obtienen por contacto
                break;
            default:
                solido = true;      // cartel, cofre, puerta y palanca bloquean
        }
    }

    /** Reparte el dibujado según el tipo. Solo en la pantalla actual. */
    public void dibujar(Graphics2D g2) {

        if (pantalla != gp.mapa.pantallaActual) return;

        switch (tipo) {
            case CARTEL:   dibujarCartel(g2);   break;
            case COFRE:    dibujarCofre(g2);    break;
            case PUERTA:   dibujarPuerta(g2);   break;
            case PALANCA:  dibujarPalanca(g2);  break;
            case MONEDA:   dibujarMoneda(g2);   break;
            case CORAZON:  UI.dibujarCorazon(g2, x + 10, y + 12, 4, 2, UI.NEGRO); break;
            case LLAVE:    dibujarLlave(g2);    break;
            case POCION:   dibujarPocion(g2);   break;
            case RELIQUIA: dibujarReliquia(g2); break;
        }
    }

    private void dibujarCartel(Graphics2D g2) {
        g2.setColor(UI.GRIS_OSCURO);
        g2.fillRect(x + 20, y + 26, 8, 18);          // poste
        g2.fillRect(x + 6, y + 6, 36, 22);           // tabla
        g2.setColor(UI.NEGRO);
        g2.drawRect(x + 6, y + 6, 36, 22);
        g2.setColor(UI.BLANCO);                       // "líneas de texto"
        g2.fillRect(x + 11, y + 12, 26, 3);
        g2.fillRect(x + 11, y + 19, 18, 3);
    }

    private void dibujarCofre(Graphics2D g2) {
        g2.setColor(UI.GRIS_OSCURO);
        g2.fillRect(x + 6, y + 12, 36, 30);          // cuerpo
        g2.setColor(UI.NEGRO);
        g2.drawRect(x + 6, y + 12, 36, 30);

        if (abierto) {
            // Interior oscuro a la vista: el botín ya voló.
            g2.fillRect(x + 10, y + 16, 28, 12);
        } else {
            g2.drawLine(x + 6, y + 24, x + 42, y + 24);  // línea de la tapa
            g2.fillRect(x + 21, y + 22, 6, 8);           // cerradura
        }
    }

    private void dibujarPuerta(Graphics2D g2) {
        g2.setColor(UI.NEGRO);
        g2.fillRect(x + 4, y + 2, 40, 44);
        g2.setColor(UI.GRIS_CLARO);
        g2.drawRect(x + 4, y + 2, 40, 44);
        g2.setColor(UI.BLANCO);                       // ojo de cerradura
        g2.fillOval(x + 20, y + 18, 8, 8);
        g2.fillRect(x + 22, y + 24, 4, 10);
    }

    private void dibujarPalanca(Graphics2D g2) {
        g2.setColor(UI.GRIS_OSCURO);
        g2.fillRect(x + 12, y + 34, 24, 8);          // base
        g2.setColor(UI.NEGRO);
        // El palo se inclina a un lado u otro según el estado.
        int paloX = activada ? x + 28 : x + 14;
        g2.fillRect(paloX, y + 14, 6, 22);
        g2.fillOval(paloX - 3, y + 8, 12, 12);       // pomo
    }

    private void dibujarMoneda(Graphics2D g2) {
        g2.setColor(UI.GRIS_CLARO);
        g2.fillOval(x + 14, y + 14, 20, 20);
        g2.setColor(UI.NEGRO);
        g2.drawOval(x + 14, y + 14, 20, 20);
        g2.drawOval(x + 19, y + 19, 10, 10);         // anillo interior
    }

    private void dibujarLlave(Graphics2D g2) {
        g2.setColor(UI.NEGRO);
        g2.drawOval(x + 14, y + 10, 14, 14);         // cabeza
        g2.fillRect(x + 19, y + 24, 5, 16);          // tallo
        g2.fillRect(x + 24, y + 32, 6, 4);           // dientes
        g2.fillRect(x + 24, y + 38, 8, 4);
    }

    private void dibujarPocion(Graphics2D g2) {
        g2.setColor(UI.GRIS_CLARO);
        g2.fillRect(x + 16, y + 18, 16, 22);         // frasco
        g2.setColor(UI.NEGRO);
        g2.drawRect(x + 16, y + 18, 16, 22);
        g2.fillRect(x + 20, y + 10, 8, 8);           // cuello y corcho
        g2.fillRect(x + 19, y + 28, 10, 10);         // líquido
    }

    private void dibujarReliquia(Graphics2D g2) {
        // Rombo brillante: el premio final.
        int cx = x + 24, cy = y + 24;
        int[] px = { cx, cx + 14, cx, cx - 14 };
        int[] py = { cy - 18, cy, cy + 18, cy };
        g2.setColor(UI.BLANCO);
        g2.fillPolygon(px, py, 4);
        g2.setColor(UI.NEGRO);
        g2.drawPolygon(px, py, 4);
        g2.drawLine(cx, cy - 18, cx, cy + 18);       // facetas
        g2.drawLine(cx - 14, cy, cx + 14, cy);
    }
}