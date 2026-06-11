import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CLASE SAVEMANAGER — Guardado y carga en un archivo de texto.
 *
 * EL PROCESO DE GUARDADO en dos palabras: serializar es convertir el
 * estado vivo del juego (objetos en memoria) en texto plano que
 * sobrevive al cierre del programa; cargar es el viaje inverso.
 *
 * Formato elegido: "clave=valor", una línea por dato, en partida.txt
 * junto al programa. ¿Por qué texto y no algo binario o JSON? Porque se
 * puede ABRIR CON EL BLOC DE NOTAS: ver el archivo, editarlo a mano y
 * romperlo a propósito es la mejor forma de entender qué es persistencia.
 *
 * vida=4
 * inventario=Espada:1,Llave:1
 * recogidos=cofre_bosque,moneda_b1
 *
 * ¿Qué guardamos? Lo mínimo que no se puede reconstruir: posición,
 * vida, monedas, inventario y los IDs de objetos ya usados. El mundo
 * (mapas, NPCs, enemigos) NO se guarda: colocarEntidades() lo recrea
 * idéntico y aplicarObjetosRecogidos() le resta lo ya consumido.
 * Consecuencia asumida: los enemigos derrotados reviven al cargar,
 * como en los clásicos.
 *
 * try-with-resources — el try (...) con paréntesis— cierra el archivo
 * automáticamente incluso si algo falla a mitad. Imprescindible al
 * escribir: un archivo sin cerrar puede quedarse a medias en disco.
 */
public class SaveManager {

    GamePanel gp;
    private static final String ARCHIVO = "partida.txt";

    public SaveManager(GamePanel gp) {
        this.gp = gp;
    }

    // ------------------------------------------------------------------
    // GUARDAR: estado → texto
    // ------------------------------------------------------------------
    public void guardar() {

        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(ARCHIVO))) {

            Player j = gp.jugador;

            escritor.write("pantalla=" + j.pantalla);        escritor.newLine();
            escritor.write("x=" + j.x);                      escritor.newLine();
            escritor.write("y=" + j.y);                      escritor.newLine();
            escritor.write("vida=" + j.vida);                escritor.newLine();
            escritor.write("vidaMaxima=" + j.vidaMaxima);    escritor.newLine();
            escritor.write("monedas=" + j.monedas);          escritor.newLine();

            // Inventario en una línea: Nombre:cantidad separados por comas.
            StringBuilder inv = new StringBuilder();
            for (int i = 0; i < j.inventario.tamano(); i++) {
                if (i > 0) inv.append(",");
                inv.append(j.inventario.nombreEn(i))
                   .append(":")
                   .append(j.inventario.cantidadEn(i));
            }
            escritor.write("inventario=" + inv);             escritor.newLine();

            // IDs de cofres abiertos, puertas destruidas, etc.
            escritor.write("recogidos=" + String.join(",", gp.objetosRecogidos));
            escritor.newLine();

        } catch (IOException e) {
            // Disco lleno, sin permisos... Avisamos sin romper el juego.
            gp.ui.mostrarMensaje("Error al guardar la partida");
        }
    }

    // ------------------------------------------------------------------
    // CARGAR: texto → estado
    // Devuelve true si todo fue bien (KeyHandler decide qué hacer).
    // ------------------------------------------------------------------
    public boolean cargar() {

        // ORDEN IMPORTANTE: primero reconstruir el mundo completo y
        // resetear al jugador; después sobrescribir con lo guardado.
        // Al revés, colocarEntidades() pisaría la partida cargada.
        gp.colocarEntidades();
        gp.objetosRecogidos.clear();
        gp.jugador.valoresIniciales();
        gp.jugador.inventario.reiniciar();   // sin la espada de regalo:
                                             // la real viene en el archivo

        try (BufferedReader lector = new BufferedReader(new FileReader(ARCHIVO))) {

            String linea;
            while ((linea = lector.readLine()) != null) {

                // split con límite 2: el valor podría contener '='.
                String[] partes = linea.split("=", 2);
                if (partes.length < 2) continue;   // línea corrupta: ignorar

                aplicarDato(partes[0], partes[1]);
            }

            // Con los IDs ya cargados, restamos del mundo lo consumido.
            gp.aplicarObjetosRecogidos();
            gp.mapa.pantallaActual = gp.jugador.pantalla;
            return true;

        } catch (IOException e) {
            // No existe partida.txt (o no se pudo leer): no es un error
            // grave, simplemente no hay nada que cargar.
            return false;
        } catch (NumberFormatException e) {
            // Alguien editó el archivo y puso "vida=hola". Mejor partida
            // nueva que un juego a medio corromper.
            gp.jugador.valoresIniciales();
            return false;
        }
    }

    /** Traduce cada pareja clave=valor a su variable del juego. */
    private void aplicarDato(String clave, String valor) {

        Player j = gp.jugador;

        switch (clave) {
            case "pantalla":   j.pantalla = Integer.parseInt(valor);   break;
            case "x":          j.x = Integer.parseInt(valor);          break;
            case "y":          j.y = Integer.parseInt(valor);          break;
            case "vida":       j.vida = Integer.parseInt(valor);       break;
            case "vidaMaxima": j.vidaMaxima = Integer.parseInt(valor); break;
            case "monedas":    j.monedas = Integer.parseInt(valor);    break;

            case "inventario":
                if (!valor.isEmpty()) {
                    // "Espada:1,Poción:2" → entradas → nombre y cantidad
                    for (String entrada : valor.split(",")) {
                        String[] par = entrada.split(":");
                        if (par.length == 2) {
                            int cantidad = Integer.parseInt(par[1]);
                            for (int n = 0; n < cantidad; n++) {
                                j.inventario.agregar(par[0]);
                            }
                        }
                    }
                }
                break;

            case "recogidos":
                if (!valor.isEmpty()) {
                    for (String id : valor.split(",")) {
                        gp.objetosRecogidos.add(id);
                    }
                }
                break;
        }
    }
}