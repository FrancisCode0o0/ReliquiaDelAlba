import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * CLASE UI — Todo lo que se dibuja ENCIMA del mundo.
 *
 * Corazones, mensajes flotantes, ventana de diálogo, inventario, título,
 * victoria y derrota. Es la última capa del renderizado y la cara
 * visible de la máquina de estados: dibujar(g2) mira gp.estado y pinta
 * lo que toca. GamePanel decide QUÉ estado es; UI decide QUÉ se ve.
 *
 * Sobre el diálogo: las líneas del NPC se guardan aquí y ENTER
 * (vía KeyHandler) llama a avanzarDialogo() para pasar página. Al
 * agotarse las líneas, devolvemos el estado a JUEGO y el mundo despierta.
 */
public class UI {

    GamePanel gp;

    // La paleta oficial, compartida por todo el proyecto (Boss y
    // GameObject la usan como UI.NEGRO, UI.BLANCO...).
    public static final Color BLANCO = new Color(0xFFFFFF);
    public static final Color GRIS_CLARO = new Color(0xAAAAAA);
    public static final Color GRIS_OSCURO = new Color(0x555555);
    public static final Color NEGRO = new Color(0x000000);

    private Font fuenteNormal = new Font("Monospaced", Font.BOLD, 20);
    private Font fuenteGrande = new Font("Monospaced", Font.BOLD, 44);

    // Estado del diálogo activo.
    private String[] lineasDialogo = null;
    private int lineaActual = 0;

    // Mensajes flotantes ("Moneda obtenida"): texto + frames de vida.
    private String mensaje = "";
    private int contadorMensaje = 0;

    public UI(GamePanel gp) {
        this.gp = gp;
    }

    /** Un NPC o un cartel entrega sus líneas: congela el mundo. */
    public void iniciarDialogo(String[] lineas) {
        lineasDialogo = lineas;
        lineaActual = 0;
        gp.estado = GamePanel.ESTADO_DIALOGO;
    }

    /** ENTER en un diálogo: siguiente línea, o cerrar y reanudar. */
    public void avanzarDialogo() {
        lineaActual++;
        if (lineasDialogo == null || lineaActual >= lineasDialogo.length) {
            lineasDialogo = null;
            gp.estado = GamePanel.ESTADO_JUEGO;
        }
    }

    /** Mensaje breve en pantalla (~2 segundos a 60 FPS). */
    public void mostrarMensaje(String texto) {
        mensaje = texto;
        contadorMensaje = 120;
    }

    /** El reparto central: cada estado tiene su dibujo. */
    public void dibujar(Graphics2D g2) {

        g2.setFont(fuenteNormal);

        switch (gp.estado) {
            case GamePanel.ESTADO_TITULO:     dibujarTitulo(g2);      return;
            case GamePanel.ESTADO_GAME_OVER:  dibujarGameOver(g2);    return;
            case GamePanel.ESTADO_VICTORIA:   dibujarVictoria(g2);    return;
        }

        // Estados "dentro del mundo": el HUD siempre visible...
        dibujarCorazones(g2);
        dibujarMonedas(g2);

        // ...y encima, lo que el estado pida.
        if (gp.estado == GamePanel.ESTADO_DIALOGO)     dibujarDialogo(g2);
        if (gp.estado == GamePanel.ESTADO_INVENTARIO)  dibujarInventario(g2);

        if (contadorMensaje > 0) {
            dibujarMensaje(g2);
            contadorMensaje--;
        }
    }

    // ------------------------------------------------------------------
    // BARRA DE VIDA EN CORAZONES
    // vida va de 2 en 2: cada corazón son 2 puntos, así que un valor
    // impar pinta medio corazón. Para cada hueco i comparamos la vida
    // con su umbral: ¿lleno, medio o vacío?
    // ------------------------------------------------------------------
    private void dibujarCorazones(Graphics2D g2) {

        int totalCorazones = gp.jugador.vidaMaxima / 2;

        for (int i = 0; i < totalCorazones; i++) {
            int px = 16 + i * 40;
            int umbral = (i + 1) * 2;   // vida necesaria para este corazón lleno

            if (gp.jugador.vida >= umbral) {
                dibujarCorazon(g2, px, 14, 6, 3, NEGRO);              // lleno
            } else if (gp.jugador.vida == umbral - 1) {
                dibujarCorazon(g2, px, 14, 6, 3, GRIS_CLARO);         // medio
            } else {
                dibujarCorazonVacio(g2, px, 14, 6, 3);                // vacío
            }
        }
    }

    /**
     * Corazón en "pixel art" con 6 rectángulos. Es static y con
     * parámetros de escala para que GameObject lo reutilice al dibujar
     * los corazones del suelo: una sola definición de corazón en todo
     * el juego.
     */
    public static void dibujarCorazon(Graphics2D g2, int px, int py,
                                      int u, int alto, Color color) {
        g2.setColor(color);
        g2.fillRect(px,         py,             u, alto);       // lóbulo izq.
        g2.fillRect(px + 2 * u, py,             u, alto);       // lóbulo der.
        g2.fillRect(px - u / 2, py + alto,      4 * u / 2 + 2 * u, alto);  // banda ancha
        g2.fillRect(px,         py + 2 * alto,  3 * u, alto);   // banda media
        g2.fillRect(px + u,     py + 3 * alto,  u, alto);       // punta
    }

    /** Versión hueca: solo el contorno, para los corazones perdidos. */
    private void dibujarCorazonVacio(Graphics2D g2, int px, int py, int u, int alto) {
        g2.setColor(GRIS_OSCURO);
        g2.drawRect(px,         py,             u, alto);
        g2.drawRect(px + 2 * u, py,             u, alto);
        g2.drawRect(px - u / 2, py + alto,      4 * u / 2 + 2 * u, alto);
        g2.drawRect(px,         py + 2 * alto,  3 * u, alto);
        g2.drawRect(px + u,     py + 3 * alto,  u, alto);
    }

    private void dibujarMonedas(Graphics2D g2) {
        g2.setColor(NEGRO);
        g2.fillOval(GamePanel.ANCHO - 120, 12, 22, 22);
        g2.setColor(BLANCO);
        g2.drawOval(GamePanel.ANCHO - 115, 17, 12, 12);
        g2.setColor(NEGRO);
        g2.drawString("x " + gp.jugador.monedas, GamePanel.ANCHO - 88, 30);
    }

    /** Caja blanca con doble borde, abajo, estilo clásico. */
    private void dibujarDialogo(Graphics2D g2) {

        int margen = 24;
        int alto = 130;
        int cy = GamePanel.ALTO - alto - margen;

        g2.setColor(BLANCO);
        g2.fillRect(margen, cy, GamePanel.ANCHO - margen * 2, alto);
        g2.setColor(NEGRO);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(margen, cy, GamePanel.ANCHO - margen * 2, alto);
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(margen + 6, cy + 6,
                GamePanel.ANCHO - margen * 2 - 12, alto - 12);

        if (lineasDialogo != null && lineaActual < lineasDialogo.length) {
            dibujarTextoPartido(g2, lineasDialogo[lineaActual],
                    margen + 24, cy + 40, GamePanel.ANCHO - margen * 2 - 48);
        }
        g2.drawString("[ENTER]", GamePanel.ANCHO - margen - 130, cy + alto - 16);
    }

    /** Parte el texto en líneas que quepan en la caja (word wrap a mano):
     * vamos sumando palabras y, cuando la línea se pasa de ancho,
     * la pintamos y empezamos otra. FontMetrics mide los píxeles. */
    private void dibujarTextoPartido(Graphics2D g2, String texto,
                                     int px, int py, int anchoMax) {
        String[] palabras = texto.split(" ");
        String linea = "";
        int y = py;

        for (String palabra : palabras) {
            String prueba = linea.isEmpty() ? palabra : linea + " " + palabra;
            if (g2.getFontMetrics().stringWidth(prueba) > anchoMax) {
                g2.drawString(linea, px, y);
                linea = palabra;
                y += 28;
            } else {
                linea = prueba;
            }
        }
        g2.drawString(linea, px, y);
    }

    private void dibujarMensaje(Graphics2D g2) {
        int ancho = g2.getFontMetrics().stringWidth(mensaje) + 30;
        int px = GamePanel.ANCHO / 2 - ancho / 2;

        g2.setColor(BLANCO);
        g2.fillRect(px, 54, ancho, 36);
        g2.setColor(NEGRO);
        g2.drawRect(px, 54, ancho, 36);
        g2.drawString(mensaje, px + 15, 79);
    }

    private void dibujarInventario(Graphics2D g2) {

        int px = GamePanel.ANCHO / 2 - 180;
        int py = 90;
        int ancho = 360, alto = 330;

        g2.setColor(BLANCO);
        g2.fillRect(px, py, ancho, alto);
        g2.setColor(NEGRO);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(px, py, ancho, alto);
        g2.setStroke(new BasicStroke(1));

        g2.drawString("— INVENTARIO —", px + 80, py + 36);

        Inventory inv = gp.jugador.inventario;

        if (inv.tamano() == 0) {
            g2.setColor(GRIS_OSCURO);
            g2.drawString("(vacío)", px + 130, py + 100);
        } else {
            for (int i = 0; i < inv.tamano(); i++) {
                int y = py + 80 + i * 34;
                if (i == inv.cursor) {
                    g2.drawString(">", px + 26, y);   // el cursor del menú
                }
                g2.drawString(inv.nombreEn(i) + "  x" + inv.cantidadEn(i),
                        px + 56, y);
            }
        }

        g2.setColor(GRIS_OSCURO);
        g2.drawString("ENTER usar   I cerrar", px + 56, py + alto - 20);
    }

    // ------------------------------------------------------------------
    // Pantallas completas (título, derrota, victoria)
    // ------------------------------------------------------------------
    private void dibujarTitulo(Graphics2D g2) {

        g2.setColor(NEGRO);
        g2.fillRect(0, 0, GamePanel.ANCHO, GamePanel.ALTO);

        g2.setColor(BLANCO);
        g2.setFont(fuenteGrande);
        centrar(g2, "LA RELIQUIA", 170);
        centrar(g2, "DEL ALBA", 225);

        // Marco decorativo de rombos.
        g2.setFont(fuenteNormal);
        centrar(g2, "◆ ─────────────── ◆", 280);

        g2.setColor(GRIS_CLARO);
        centrar(g2, "ENTER — Nueva partida", 360);
        centrar(g2, "C — Cargar partida", 400);

        g2.setColor(GRIS_OSCURO);
        centrar(g2, "Flechas/WASD mover · ESPACIO espada", 480);
        centrar(g2, "ENTER hablar · I inventario · G guardar", 510);
    }

    private void dibujarGameOver(Graphics2D g2) {
        g2.setColor(NEGRO);
        g2.fillRect(0, 0, GamePanel.ANCHO, GamePanel.ALTO);

        g2.setColor(BLANCO);
        g2.setFont(fuenteGrande);
        centrar(g2, "HAS CAÍDO", 240);

        g2.setFont(fuenteNormal);
        g2.setColor(GRIS_CLARO);
        centrar(g2, "La oscuridad cubre el alba...", 310);
        centrar(g2, "ENTER — Volver al título", 400);
    }

    private void dibujarVictoria(Graphics2D g2) {
        g2.setColor(BLANCO);
        g2.fillRect(0, 0, GamePanel.ANCHO, GamePanel.ALTO);

        g2.setColor(NEGRO);
        g2.setFont(fuenteGrande);
        centrar(g2, "¡VICTORIA!", 220);

        g2.setFont(fuenteNormal);
        centrar(g2, "La Reliquia del Alba brilla de nuevo.", 300);
        centrar(g2, "La aldea está a salvo gracias a ti.", 335);
        g2.setColor(GRIS_OSCURO);
        centrar(g2, "ENTER — Volver al título", 430);
    }

    /** Centra un texto horizontalmente midiendo su ancho en píxeles. */
    private void centrar(Graphics2D g2, String texto, int y) {
        int ancho = g2.getFontMetrics().stringWidth(texto);
        g2.drawString(texto, GamePanel.ANCHO / 2 - ancho / 2, y);
    }
}