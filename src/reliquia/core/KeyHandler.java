package reliquia.core;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * CLASE KEYHANDLER — Gestión de eventos del teclado.
 *
 * Swing funciona por EVENTOS: cuando el usuario pulsa una tecla, Java
 * llama automáticamente a keyPressed(); cuando la suelta, a keyReleased().
 * Nosotros NO preguntamos por el teclado: el teclado nos avisa.
 *
 * El truco para conectar eventos con el bucle del juego son las BANDERAS
 * (booleanos):
 *
 *  - Teclas de MOVIMIENTO: la bandera está en true MIENTRAS la tecla
 *    esté pulsada (true en keyPressed, false en keyReleased). Así el
 *    héroe camina de forma continua.
 *
 *  - Teclas de ACCIÓN (ENTER, ESPACIO): solo nos interesa la pulsación,
 *    no cuánto dura. La ponemos en true aquí, y quien la usa (Player)
 *    la devuelve a false tras consumirla. Patrón "usar y apagar".
 *
 * Además, este oyente respeta la máquina de estados: la misma tecla
 * hace cosas distintas según dónde esté el juego (ENTER inicia partida
 * en el título, pero avanza el texto en un diálogo).
 */
public class KeyHandler implements KeyListener {

    GamePanel gp;

    // Banderas de movimiento (continuas).
    public boolean arriba, abajo, izquierda, derecha;

    // Banderas de un solo uso (las consume Player).
    public boolean accion;   // ENTER: hablar / abrir / confirmar
    public boolean ataque;   // ESPACIO: golpe de espada
    public boolean soltar;

    public KeyHandler(GamePanel gp) {
        this.gp = gp;
    }

    /** keyTyped sirve para texto escrito (letras con tilde, etc.).
     *  En un juego de acción no lo necesitamos, pero la interfaz
     *  KeyListener nos obliga a declararlo. Lo dejamos vacío. */
    @Override
    public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {

        int codigo = e.getKeyCode();  // número que identifica la tecla

        // La máquina de estados decide qué significan las teclas AHORA.
        switch (gp.estado) {
            case GamePanel.ESTADO_TITULO:
                teclasDelTitulo(codigo);
                break;

            case GamePanel.ESTADO_JUEGO:
                teclasDelJuego(codigo);
                break;

            case GamePanel.ESTADO_DIALOGO:
                if (codigo == KeyEvent.VK_ENTER) {
                    gp.ui.avanzarDialogo();  // siguiente línea o cerrar
                }
                break;

            case GamePanel.ESTADO_INVENTARIO:
                teclasDelInventario(codigo);
                break;

            case GamePanel.ESTADO_GAME_OVER:
            case GamePanel.ESTADO_VICTORIA:
                if (codigo == KeyEvent.VK_ENTER) {
                    gp.estado = GamePanel.ESTADO_TITULO;  // volver al título
                }
                break;
        }
    }

    private void teclasDelTitulo(int codigo) {
        if (codigo == KeyEvent.VK_ENTER) {
            gp.nuevaPartida();
        }
        if (codigo == KeyEvent.VK_C) {
            // cargar() devuelve true si existía partida guardada.
            if (gp.guardado.cargar()) {
                gp.estado = GamePanel.ESTADO_JUEGO;
                gp.ui.mostrarMensaje("Partida cargada");
            } else {
                gp.ui.mostrarMensaje("No hay partida guardada");
            }
        }
    }

    private void teclasDelJuego(int codigo) {

        // Aceptamos flechas Y la disposición WASD: comodidad del jugador.
        if (codigo == KeyEvent.VK_UP    || codigo == KeyEvent.VK_W) arriba = true;
        if (codigo == KeyEvent.VK_DOWN  || codigo == KeyEvent.VK_S) abajo = true;
        if (codigo == KeyEvent.VK_LEFT  || codigo == KeyEvent.VK_A) izquierda = true;
        if (codigo == KeyEvent.VK_RIGHT || codigo == KeyEvent.VK_D) derecha = true;

        if (codigo == KeyEvent.VK_SPACE) ataque = true;
        if (codigo == KeyEvent.VK_ENTER) accion = true;
        if (codigo == KeyEvent.VK_Q) soltar = true;

        if (codigo == KeyEvent.VK_I) {
            gp.estado = GamePanel.ESTADO_INVENTARIO;
        }
        if (codigo == KeyEvent.VK_G) {
            gp.guardado.guardar();
            gp.ui.mostrarMensaje("Partida guardada");
        }
    }

    private void teclasDelInventario(int codigo) {
        if (codigo == KeyEvent.VK_I || codigo == KeyEvent.VK_ESCAPE) {
            gp.estado = GamePanel.ESTADO_JUEGO;  // cerrar inventario
        }
        if (codigo == KeyEvent.VK_UP   || codigo == KeyEvent.VK_W) {
            gp.jugador.inventario.moverCursor(-1);
        }
        if (codigo == KeyEvent.VK_DOWN || codigo == KeyEvent.VK_S) {
            gp.jugador.inventario.moverCursor(1);
        }
        if (codigo == KeyEvent.VK_ENTER) {
            gp.jugador.inventario.usarSeleccionado();  // ej: beber poción
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int codigo = e.getKeyCode();

        // Al soltar, el movimiento se detiene. Las banderas de acción
        // no se tocan aquí: las apaga quien las consume.
        if (codigo == KeyEvent.VK_UP    || codigo == KeyEvent.VK_W) arriba = false;
        if (codigo == KeyEvent.VK_DOWN  || codigo == KeyEvent.VK_S) abajo = false;
        if (codigo == KeyEvent.VK_LEFT  || codigo == KeyEvent.VK_A) izquierda = false;
        if (codigo == KeyEvent.VK_RIGHT || codigo == KeyEvent.VK_D) derecha = false;
    }
}