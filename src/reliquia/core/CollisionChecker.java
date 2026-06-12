package reliquia.core;

import java.awt.Rectangle;
import java.util.ArrayList;
import reliquia.entity.Entity;
import reliquia.object.GameObject;
/**
 * CLASE COLLISIONCHECKER — La detección de colisiones.
 *
 * Idea central: PREDECIR, NO CORREGIR. Antes de mover una entidad,
 * calculamos dónde ESTARÍA su hitbox tras el paso y comprobamos si ese
 * futuro choca con algo. Si choca, encendemos colisionDetectada y la
 * entidad simplemente no da el paso. Nunca hay que "sacar" a nadie de
 * dentro de un muro porque nadie llega a entrar.
 *
 * Hay dos familias de comprobación:
 *
 * 1. CONTRA TILES (revisarTile): convertimos píxeles a casillas
 * dividiendo por TAMANO_TILE y consultamos la matriz del mapa.
 * Probamos TRES puntos del borde de avance (dos esquinas + centro):
 * con dos esquinas bastaría para el héroe, pero un borde más ancho
 * que un tile (la Bestia mide 80 px) puede abarcar tres columnas y
 * colarse por la del medio. El punto central tapa ese hueco.
 *
 * 2. CONTRA RECTÁNGULOS (objetos y NPCs): aquí brilla el método
 * intersects() de la clase Rectangle, que Java ya nos regala.
 * Calculamos el hitbox futuro y preguntamos si se solapa.
 */
public class CollisionChecker {

    GamePanel gp;

    public CollisionChecker(GamePanel gp) {
        this.gp = gp;
    }

    /** Colisión entidad ↔ tiles del mapa. */
    public void revisarTile(Entity entidad) {

        // Hitbox actual en coordenadas absolutas.
        int izquierda = entidad.x + entidad.areaSolida.x;
        int derecha   = izquierda + entidad.areaSolida.width;
        int arriba    = entidad.y + entidad.areaSolida.y;
        int abajo     = arriba + entidad.areaSolida.height;

        int v = entidad.velocidad;
        int t = GamePanel.TAMANO_TILE;

        // Según hacia dónde mira, examinamos el borde que avanza.
        switch (entidad.direccion) {

            case "arriba": {
                int filaFutura = (arriba - v) / t;   // píxeles → casilla
                if (solidoEnFila(filaFutura, izquierda, derecha, t)) {
                    entidad.colisionDetectada = true;
                }
                break;
            }
            case "abajo": {
                int filaFutura = (abajo + v) / t;
                if (solidoEnFila(filaFutura, izquierda, derecha, t)) {
                    entidad.colisionDetectada = true;
                }
                break;
            }
            case "izquierda": {
                int colFutura = (izquierda - v) / t;
                if (solidoEnColumna(colFutura, arriba, abajo, t)) {
                    entidad.colisionDetectada = true;
                }
                break;
            }
            case "derecha": {
                int colFutura = (derecha + v) / t;
                if (solidoEnColumna(colFutura, arriba, abajo, t)) {
                    entidad.colisionDetectada = true;
                }
                break;
            }
        }
    }

    /** ¿Algún punto del borde horizontal (esquinas + centro) pisa sólido? */
    private boolean solidoEnFila(int fila, int izquierda, int derecha, int t) {
        int centro = (izquierda + derecha) / 2;
        return gp.mapa.esSolido(izquierda / t, fila)
            || gp.mapa.esSolido(centro / t, fila)
            || gp.mapa.esSolido(derecha / t, fila);
    }

    /** Igual, para el borde vertical. */
    private boolean solidoEnColumna(int col, int arriba, int abajo, int t) {
        int centro = (arriba + abajo) / 2;
        return gp.mapa.esSolido(col, arriba / t)
            || gp.mapa.esSolido(col, centro / t)
            || gp.mapa.esSolido(col, abajo / t);
    }

    /**
     * Colisión entidad ↔ objetos del escenario.
     * Devuelve el ÍNDICE del objeto tocado (-1 si ninguno): el doble
     * servicio del método. Para los sólidos frena; para los recogibles
     * solo informa, y Player decide recogerlos.
     */
    public int revisarObjetos(Entity entidad) {

        int indiceTocado = -1;

        for (int i = 0; i < gp.objetos.size(); i++) {
            GameObject objeto = gp.objetos.get(i);

            if (objeto.pantalla != gp.mapa.pantallaActual) continue;

            Rectangle areaFutura = areaFutura(entidad);
            Rectangle areaObjeto = new Rectangle(objeto.x, objeto.y,
                    GamePanel.TAMANO_TILE, GamePanel.TAMANO_TILE);

            if (areaFutura.intersects(areaObjeto)) {
                if (objeto.solido) {
                    entidad.colisionDetectada = true;
                }
                indiceTocado = i;
            }
        }
        return indiceTocado;
    }

    /** Colisión entidad ↔ otras entidades (el héroe no atraviesa NPCs). */
    public void revisarEntidades(Entity entidad, ArrayList<? extends Entity> lista) {

        for (Entity otra : lista) {
            if (otra == entidad) continue;  // nadie choca consigo mismo
            if (otra.pantalla != gp.mapa.pantallaActual) continue;

            if (areaFutura(entidad).intersects(otra.areaMundo())) {
                entidad.colisionDetectada = true;
            }
        }
    }

    /** El hitbox donde ESTARÍA la entidad tras dar su próximo paso. */
    private Rectangle areaFutura(Entity entidad) {

        Rectangle area = entidad.areaMundo();

        switch (entidad.direccion) {
            case "arriba":    area.y -= entidad.velocidad; break;
            case "abajo":     area.y += entidad.velocidad; break;
            case "izquierda": area.x -= entidad.velocidad; break;
            case "derecha":   area.x += entidad.velocidad; break;
        }
        return area;
    }
}