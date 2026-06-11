import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * CLASE ENTITY — Madre de todo ser viviente del juego.
 *
 * Player, Enemy, Boss y NPC HEREDAN de esta clase. La herencia nos
 * permite escribir UNA sola vez lo que todos comparten: posición,
 * vida, sprites, animación, hitbox e invencibilidad temporal.
 *
 * Dos ideas importantes aquí:
 *
 *  1. SPRITES POR CÓDIGO: crearSprite() convierte una matriz de
 *     caracteres en una imagen. Cada carácter es un píxel:
 *        '.' transparente   '0' blanco   '1' gris claro
 *        '2' gris oscuro    '3' negro
 *     Así "dibujamos" sin necesitar archivos de imagen externos.
 *
 *  2. HITBOX (areaSolida): el rectángulo de colisión es MÁS PEQUEÑO
 *     que el sprite. Si chocara todo el cuadrado de 48x48, el héroe
 *     se atascaría en las esquinas. Un hitbox ajustado al cuerpo hace
 *     que moverse se sienta justo.
 *
 * Nota de diseño: usamos atributos públicos para simplificar la lectura.
 * En un proyecto profesional irían privados con getters/setters
 * (encapsulamiento), como veréis en clase.
 */
public class Entity {

    protected GamePanel gp;

    public int x, y;                    // posición en píxeles dentro de la pantalla
    public int pantalla = 0;            // en cuál de las 5 pantallas vive
    public int velocidad = 1;           // píxeles que avanza por frame
    public String direccion = "abajo";  // hacia dónde mira

    // Un sprite por dirección. 'izquierda' suele ser un espejo de 'derecha'.
    public BufferedImage spriteArriba, spriteAbajo, spriteIzquierda, spriteDerecha;

    // Animación de caminar: alternamos un pequeño "salto" cada pocos frames.
    protected int contadorAnimacion = 0;
    protected boolean pasoAlterno = false;

    // Hitbox: x,y son el DESPLAZAMIENTO respecto a la esquina del sprite.
    public Rectangle areaSolida = new Rectangle(8, 16, 32, 28);
    public boolean colisionDetectada = false;  // la enciende CollisionChecker

    // Vida: 2 puntos = 1 corazón (permite mostrar medios corazones).
    public int vidaMaxima = 2;
    public int vida = 2;

    // Invencibilidad temporal tras recibir daño ("frames de gracia"):
    // evita perder toda la vida en un solo roce con un enemigo.
    public boolean invencible = false;
    protected int contadorInvencible = 0;

    public Entity(GamePanel gp) {
        this.gp = gp;
    }

    /** Cada subclase define su propio comportamiento por frame. */
    public void update() { }

    /** Comodidad: colocar la entidad usando coordenadas de cuadrícula. */
    public void colocarEnTile(int columna, int fila) {
        x = columna * GamePanel.TAMANO_TILE;
        y = fila * GamePanel.TAMANO_TILE;
    }

    /** Mueve la entidad según su dirección actual. OJO: aquí no se
     *  comprueban colisiones; eso lo decide cada subclase ANTES de llamar. */
    protected void moverSegunDireccion() {
        switch (direccion) {
            case "arriba":    y -= velocidad; break;
            case "abajo":     y += velocidad; break;
            case "izquierda": x -= velocidad; break;
            case "derecha":   x += velocidad; break;
        }
    }

    /** Alterna el paso de la animación cada 12 frames (~5 veces/segundo). */
    protected void animar() {
        contadorAnimacion++;
        if (contadorAnimacion > 12) {
            pasoAlterno = !pasoAlterno;
            contadorAnimacion = 0;
        }
    }

    /** Recibir daño respeta la invencibilidad temporal. */
    public void recibirDanio(int cantidad) {
        if (!invencible) {
            vida -= cantidad;
            invencible = true;
            contadorInvencible = 0;
        }
    }

    /** Cuenta los frames de invencibilidad (40 frames ≈ 0,7 segundos). */
    protected void actualizarInvencibilidad() {
        if (invencible) {
            contadorInvencible++;
            if (contadorInvencible > 40) {
                invencible = false;
                contadorInvencible = 0;
            }
        }
    }

    /** Hitbox en coordenadas absolutas de pantalla. Lo usan las
     *  colisiones entre entidades (espada vs enemigo, roce, etc.). */
    public Rectangle areaMundo() {
        return new Rectangle(x + areaSolida.x, y + areaSolida.y,
                areaSolida.width, areaSolida.height);
    }

    /** Dibuja la entidad: solo si está en la pantalla actual, con
     *  parpadeo si es invencible y un leve balanceo al caminar. */
    public void draw(Graphics2D g2) {

        if (pantalla != gp.mapa.pantallaActual) return;

        // Parpadeo: durante la invencibilidad, saltarse el dibujado
        // unos frames sí y otros no crea el clásico efecto de daño.
        if (invencible && (contadorInvencible / 4) % 2 == 0) return;

        BufferedImage sprite = elegirSprite();
        int balanceo = pasoAlterno ? -2 : 0;  // 2 px arriba en el paso alterno

        // drawImage escala el sprite de 16x16 a 48x48 al dibujarlo.
        g2.drawImage(sprite, x, y + balanceo,
                GamePanel.TAMANO_TILE, GamePanel.TAMANO_TILE, null);
    }

    /** Devuelve el sprite que corresponde a la dirección actual. */
    protected BufferedImage elegirSprite() {
        switch (direccion) {
            case "arriba":    return spriteArriba;
            case "izquierda": return spriteIzquierda;
            case "derecha":   return spriteDerecha;
            default:          return spriteAbajo;
        }
    }

    // ------------------------------------------------------------------
    // FÁBRICA DE SPRITES
    // Recorre la matriz de caracteres y pinta píxel a píxel con setRGB.
    // El color va en formato ARGB hexadecimal: los dos primeros dígitos
    // son la opacidad (FF = opaco, 00 = transparente).
    // ------------------------------------------------------------------
    protected BufferedImage crearSprite(String[] patron) {

        int alto = patron.length;
        int ancho = patron[0].length();
        BufferedImage imagen = new BufferedImage(ancho, alto,
                BufferedImage.TYPE_INT_ARGB);

        for (int fila = 0; fila < alto; fila++) {
            for (int col = 0; col < ancho; col++) {

                char c = patron[fila].charAt(col);
                int color;

                switch (c) {
                    case '0': color = 0xFFFFFFFF; break;  // blanco
                    case '1': color = 0xFFAAAAAA; break;  // gris claro
                    case '2': color = 0xFF555555; break;  // gris oscuro
                    case '3': color = 0xFF000000; break;  // negro
                    default:  color = 0x00000000; break;  // '.' transparente
                }
                imagen.setRGB(col, fila, color);
            }
        }
        return imagen;
    }

    /** Crea el reflejo horizontal de un sprite. Así el sprite "izquierda"
     *  sale gratis a partir del de "derecha": menos matrices que escribir. */
    protected BufferedImage crearEspejo(BufferedImage original) {

        int ancho = original.getWidth();
        int alto = original.getHeight();
        BufferedImage espejo = new BufferedImage(ancho, alto,
                BufferedImage.TYPE_INT_ARGB);

        for (int fila = 0; fila < alto; fila++) {
            for (int col = 0; col < ancho; col++) {
                // El píxel de la columna 'col' va a la columna opuesta.
                espejo.setRGB(ancho - 1 - col, fila, original.getRGB(col, fila));
            }
        }
        return espejo;
    }
}