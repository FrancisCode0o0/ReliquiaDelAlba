import javax.swing.JFrame;

/**
 * CLASE MAIN — Punto de entrada del programa.
 *
 * Su única responsabilidad es crear la ventana (JFrame) y meter dentro
 * el panel del juego (GamePanel). Separar "ventana" de "juego" es una
 * decisión de diseño: si mañana queremos el juego en otra ventana o en
 * pantalla completa, no tocamos ni una línea de la lógica del juego.
 */
public class Main {

    public static void main(String[] args) {

        // La ventana del sistema operativo.
        JFrame ventana = new JFrame("La Reliquia del Alba");

        // Al cerrar la ventana, termina el programa entero.
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Tamaño fijo: nuestro juego tiene resolución fija (768x576),
        // permitir redimensionar deformaría los píxeles.
        ventana.setResizable(false);

        // El panel donde ocurre TODO el juego.
        GamePanel panel = new GamePanel();
        ventana.add(panel);

        // pack() ajusta la ventana al tamaño preferido del panel.
        ventana.pack();

        // null = centrar la ventana en la pantalla del usuario.
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);

        // Arranca el hilo del juego (el bucle principal).
        panel.iniciar();
    }
}