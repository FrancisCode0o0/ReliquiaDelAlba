package reliquia.object;
import java.util.ArrayList;

import reliquia.entity.Player;

/**
 * CLASE INVENTORY — El inventario del héroe.
 *
 * Estructura elegida: dos ArrayList PARALELAS (nombres y cantidades)
 * donde el índice i de una corresponde al de la otra. ¿Por qué no un
 * HashMap, que sería lo "canónico"? Tres razones didácticas: las listas
 * conservan el ORDEN de obtención (importa al dibujar el menú), el
 * cursor del menú es un simple índice, y ArrayList es la estructura que
 * dominaréis primero. Cuando veáis mapas, reescribirlo es buen ejercicio.
 *
 * El inventario también ES la pantalla de inventario lógica: la UI le
 * pregunta qué mostrar y KeyHandler le pasa el cursor. Datos aquí,
 * dibujo en UI.
 */
public class Inventory {

    private Player jugador;

    private ArrayList<String> nombres = new ArrayList<>();
    private ArrayList<Integer> cantidades = new ArrayList<>();

    public int cursor = 0;   // qué objeto está seleccionado en el menú

    public Inventory(Player jugador) {
        this.jugador = jugador;
    }

    /** Vaciar todo (al empezar partida nueva o cargar). */
    public void reiniciar() {
        nombres.clear();
        cantidades.clear();
        cursor = 0;
    }

    /** Añadir un objeto: si ya existe, suma 1; si no, nueva entrada. */
    public void agregar(String nombre) {
        int i = nombres.indexOf(nombre);   // -1 si no está
        if (i != -1) {
            cantidades.set(i, cantidades.get(i) + 1);
        } else {
            nombres.add(nombre);
            cantidades.add(1);
        }
    }

    /** ¿Hay al menos uno? (¿tiene espada? ¿tiene llave?) */
    public boolean tiene(String nombre) {
        return nombres.indexOf(nombre) != -1;
    }

    /** Consumir una unidad; si llega a 0, la entrada desaparece. */
    public void quitarUno(String nombre) {
        int i = nombres.indexOf(nombre);
        if (i == -1) return;

        cantidades.set(i, cantidades.get(i) - 1);
        if (cantidades.get(i) <= 0) {
            nombres.remove(i);
            cantidades.remove(i);
            if (cursor >= nombres.size()) {   // que el cursor no quede colgando
                cursor = Math.max(0, nombres.size() - 1);
            }
        }
    }

    /** Mover el cursor sin salirse de la lista (lo llama KeyHandler). */
    public void moverCursor(int delta) {
        if (nombres.isEmpty()) return;
        cursor = Math.max(0, Math.min(cursor + delta, nombres.size() - 1));
    }

    /** ENTER sobre un objeto: solo la poción tiene "uso" por ahora.
     * Ejercicio propuesto: ¿qué haría usar la espada desde aquí? */
    public void usarSeleccionado() {
        if (nombres.isEmpty()) return;

        String nombre = nombres.get(cursor);

        if (nombre.equals("Poción")) {
            if (jugador.vida < jugador.vidaMaxima) {
                jugador.vida = Math.min(jugador.vida + 4, jugador.vidaMaxima);
                quitarUno("Poción");
                jugador.gp.ui.mostrarMensaje("Bebes la poción. ¡Salud restaurada!");
            } else {
                jugador.gp.ui.mostrarMensaje("Tu salud ya está completa.");
            }
        }
    }

    // --- Acceso de solo lectura para UI y SaveManager ---
    public int tamano()              { return nombres.size(); }
    public String nombreEn(int i)    { return nombres.get(i); }
    public int cantidadEn(int i)     { return cantidades.get(i); }
}