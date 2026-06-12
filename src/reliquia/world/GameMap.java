package reliquia.world;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import reliquia.core.GamePanel;
import reliquia.entity.Player;

/**
 * CLASE GAMEMAP — El mundo: 5 pantallas hechas de tiles.
 *
 * UN MUNDO DE TILES: cada pantalla es una cuadrícula de 16x12 casillas
 * y cada casilla tiene un tipo (0 suelo, 1 muro, 2 agua, 3 árbol). Lo
 * escribimos como Strings de dígitos —legibles y editables a mano— y el
 * constructor los convierte a una matriz de enteros tiles[pantalla][fila][col].
 * Esta matriz es a la vez el DIBUJO del mundo y su FÍSICA: el renderizado
 * la recorre para pintar, y CollisionChecker la consulta para frenar.
 * Una sola fuente de verdad.
 *
 * PANTALLAS CONECTADAS: la tabla 'conexiones' dice qué pantalla hay en
 * cada dirección (-1 = nada). Cuando el jugador cruza un borde, Player
 * llama a transicion(): cambiamos pantallaActual y lo recolocamos en el
 * borde opuesto. Las aberturas de los muros están ALINEADAS entre
 * pantallas vecinas (misma columna/fila); si las moviera, el jugador
 * aparecería dentro de un muro.
 */
public class GameMap {

    GamePanel gp;
    public int pantallaActual = 0;
    public static final int NUM_PANTALLAS = 5;

    // tiles[pantalla][fila][columna] → tipo de casilla
    private final int[][][] tiles =
            new int[NUM_PANTALLAS][GamePanel.MAX_FILAS][GamePanel.MAX_COLUMNAS];

    private final BufferedImage[] spritesTile = new BufferedImage[4];

    private final String[] nombres = {
        "Aldea del Alba", "Bosque Umbrío", "Lago Quieto",
        "Mazmorra", "Sala de la Bestia"
    };

    // conexiones[pantalla] = { norte, sur, este, oeste }, -1 si no hay.
    private final int[][] conexiones = {
        { -1, -1,  1, -1 },   // 0 Aldea: este → Bosque
        {  2, -1, -1,  0 },   // 1 Bosque: norte → Lago, oeste → Aldea
        {  3,  1, -1, -1 },   // 2 Lago: norte → Mazmorra, sur → Bosque
        {  4,  2, -1, -1 },   // 3 Mazmorra: norte → Jefe, sur → Lago
        { -1,  3, -1, -1 }    // 4 Sala del Jefe: sur → Mazmorra
    };

    // ------------------------------------------------------------------
    // DISEÑO DE LAS PANTALLAS (0 suelo · 1 muro · 2 agua · 3 árbol)
    // Las aberturas en los bordes son las "puertas" entre pantallas.
    // ------------------------------------------------------------------
    private static final String[] MAPA_ALDEA = {
        "1111111111111111",
        "1000000000000001",
        "1011100000111001",
        "1011100000111001",
        "1000000000000001",
        "1000000000000000",   // ← salida este (filas 5 y 6)
        "1000000000000000",
        "1030000000003001",
        "1000000022000001",
        "1000000022000001",
        "1000000000000001",
        "1111111111111111"
    };

    private static final String[] MAPA_BOSQUE = {
        "1111111001111111",   // ← salida norte (columnas 7-8)
        "1000300000030001",
        "1030000000000301",
        "1000003000000001",
        "1000000000300001",
        "0000300000000001",   // ← entrada oeste (filas 5 y 6)
        "0000000003000001",
        "1003000000000301",
        "1000000300000001",
        "1000000000000001",
        "1030000000030001",
        "1111111111111111"
    };

    private static final String[] MAPA_LAGO = {
        "1111111011111111",   // ← salida norte (columna 7, tapada por puerta)
        "1000000000000001",
        "1000000000000001",
        "1000022222000001",
        "1000222222200001",
        "1000222222200001",
        "1000022222000001",
        "1000000000000001",
        "1000000000000001",
        "1000000000000001",
        "1000000000000001",
        "1111111001111111"    // ← entrada sur (columnas 7-8)
    };

    private static final String[] MAPA_MAZMORRA = {
        "1111111101111111",   // ← salida norte (columna 8, puerta del jefe)
        "1000000000000001",
        "1000000000000001",
        "1001111001111001",
        "1000000000000001",
        "1000000000000001",
        "1001110000111001",
        "1000000000000001",
        "1000000000000001",
        "1001111001110001",
        "1000000000000001",
        "1111111011111111"    // ← entrada sur (columna 7)
    };

    private static final String[] MAPA_JEFE = {
        "1111111111111111",
        "1000000000000001",
        "1000000000000001",
        "1000000000000001",
        "1001000000001001",   // pilares de la arena
        "1000000000000001",
        "1000000000000001",
        "1001000000001001",
        "1000000000000001",
        "1000000000000001",
        "1000000000000001",
        "1111111101111111"    // ← entrada sur (columna 8)
    };

    public GameMap(GamePanel gp) {
        this.gp = gp;
        crearSpritesDeTiles();

        String[][] disenos = { MAPA_ALDEA, MAPA_BOSQUE, MAPA_LAGO,
                               MAPA_MAZMORRA, MAPA_JEFE };

        // Conversión String → matriz numérica. El truco charAt(c) - '0'
        // convierte el carácter '2' en el entero 2 (resta de códigos).
        for (int p = 0; p < NUM_PANTALLAS; p++) {
            for (int fila = 0; fila < GamePanel.MAX_FILAS; fila++) {
                for (int col = 0; col < GamePanel.MAX_COLUMNAS; col++) {
                    tiles[p][fila][col] = disenos[p][fila].charAt(col) - '0';
                }
            }
        }
    }

    /** ¿Esa casilla bloquea el paso? Fuera de los límites devolvemos
     *  false a propósito: así el jugador puede SALIR de la pantalla por
     *  las aberturas y disparar la transición. */
    public boolean esSolido(int columna, int fila) {
        if (columna < 0 || columna >= GamePanel.MAX_COLUMNAS
                || fila < 0 || fila >= GamePanel.MAX_FILAS) {
            return false;
        }
        return tiles[pantallaActual][fila][columna] != 0;  // solo el suelo es libre
    }

    /** RENDERIZADO del mapa: dos bucles anidados, una imagen por casilla.
     *  Es la primera capa que se pinta en cada frame. */
    public void dibujar(Graphics2D g2) {
        int t = GamePanel.TAMANO_TILE;
        for (int fila = 0; fila < GamePanel.MAX_FILAS; fila++) {
            for (int col = 0; col < GamePanel.MAX_COLUMNAS; col++) {
                int tipo = tiles[pantallaActual][fila][col];
                g2.drawImage(spritesTile[tipo], col * t, fila * t, t, t, null);
            }
        }
    }

    /** Cambia de pantalla y recoloca al héroe en el borde opuesto:
     *  si sale por el este, entra por el oeste de la siguiente. */
    public void transicion(String direccion) {

        int indice;
        switch (direccion) {
            case "norte": indice = 0; break;
            case "sur":   indice = 1; break;
            case "este":  indice = 2; break;
            default:      indice = 3; break;  // oeste
        }

        int destino = conexiones[pantallaActual][indice];

        Player j = gp.jugador;
        int t = GamePanel.TAMANO_TILE;

        if (destino == -1) {
            // Defensa: no debería ocurrir (los bordes son muros),
            // pero si pasa, devolvemos al jugador adentro.
            j.x = Math.max(0, Math.min(j.x, GamePanel.ANCHO - t));
            j.y = Math.max(0, Math.min(j.y, GamePanel.ALTO - t));
            return;
        }

        pantallaActual = destino;
        j.pantalla = destino;   // ¡importante! si no, el héroe no se dibuja

        switch (direccion) {
            case "este":  j.x = 0; break;
            case "oeste": j.x = GamePanel.ANCHO - t; break;
            case "sur":   j.y = 0; break;
            case "norte": j.y = GamePanel.ALTO - t; break;
        }

        gp.ui.mostrarMensaje(nombres[destino]);
    }

    // ------------------------------------------------------------------
    // Sprites de los tiles. El método crearTile duplica la fábrica de
    // Entity porque GameMap no hereda de ella. Ejercicio propuesto:
    // extraer ambas a una clase utilitaria SpriteFactory.
    // ------------------------------------------------------------------
    private void crearSpritesDeTiles() {

        String[] suelo = {
            "0000000000000000","0000000000000000","0000010000000000",
            "0000000000000000","0000000000001000","0000000000000000",
            "0010000000000000","0000000000000000","0000000001000000",
            "0000000000000000","0000100000000000","0000000000000000",
            "0000000000010000","0000000000000000","0100000000000000",
            "0000000000000000"
        };

        String[] muro = {   // ladrillos: líneas de mortero y juntas alternas
            "3333333333333333","2222222322222223","2222222322222223",
            "2222222322222223","3333333333333333","2232222222322222",
            "2232222222322222","2232222222322222","3333333333333333",
            "2222222322222223","2222222322222223","2222222322222223",
            "3333333333333333","2232222222322222","2232222222322222",
            "2232222222322222"
        };

        String[] agua = {   // gris con destellos blancos de oleaje
            "1111111111111111","1111101111111111","1111011111111011",
            "1111111111110111","1111111111111111","1011111111111111",
            "0111111101111111","1111111011111111","1111111111111111",
            "1111110111111101","1111101111111011","1111111111111111",
            "1101111111111111","1011111110111111","1111111111011111",
            "1111111111111111"
        };

        String[] arbol = {
            "0000000000000000","0000223333220000","0002333333332000",
            "0023333333333200","0233333223333320","0233332332333320",
            "0233333333333320","0023333333333200","0002333333332000",
            "0000223333220000","0000003223000000","0000003223000000",
            "0000003223000000","0000023333200000","0000000000000000",
            "0000000000000000"
        };

        spritesTile[0] = crearTile(suelo);
        spritesTile[1] = crearTile(muro);
        spritesTile[2] = crearTile(agua);
        spritesTile[3] = crearTile(arbol);
    }

    private BufferedImage crearTile(String[] patron) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int fila = 0; fila < 16; fila++) {
            for (int col = 0; col < 16; col++) {
                int color;
                switch (patron[fila].charAt(col)) {
                    case '0': color = 0xFFFFFFFF; break;
                    case '1': color = 0xFFAAAAAA; break;
                    case '2': color = 0xFF555555; break;
                    default:  color = 0xFF000000; break;
                }
                img.setRGB(col, fila, color);
            }
        }
        return img;
    }
}