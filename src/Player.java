import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * CLASE PLAYER — El héroe.
 *
 * Hereda de Entity todo lo común (posición, vida, sprites, hitbox) y
 * añade lo que solo el jugador hace:
 *
 *  - Leer las banderas del teclado para moverse.
 *  - Atacar con la espada (un área de golpe delante del héroe).
 *  - Interactuar con NPCs, carteles, cofres, puertas y palancas.
 *  - Recoger objetos al pisarlos.
 *  - Cambiar de pantalla al cruzar los bordes del mapa.
 *
 * Su update() es el guion de un frame de la vida del héroe; léelo
 * de arriba a abajo y entenderás el flujo completo.
 */
public class Player extends Entity {

    KeyHandler teclado;

    public Inventory inventario = new Inventory(this);
    public int monedas = 0;

    // Estado del ataque: dura unos frames durante los cuales la espada
    // está "activa" y puede golpear enemigos.
    public boolean atacando = false;
    private int contadorAtaque = 0;
    private static final int DURACION_ATAQUE = 16;  // frames (~0,26 s)

    public Player(GamePanel gp, KeyHandler teclado) {
        super(gp);
        this.teclado = teclado;

        velocidad = 3;  // 3 px/frame a 60 FPS: cruza la pantalla en ~4 s
        areaSolida = new Rectangle(10, 18, 28, 26);  // hitbox ajustado al cuerpo

        cargarSprites();
        valoresIniciales();
    }

    /** Estado de una partida nueva. También lo usa GamePanel.nuevaPartida(). */
    public void valoresIniciales() {
        pantalla = 0;             // empieza en la aldea
        colocarEnTile(7, 6);
        direccion = "abajo";

        vidaMaxima = 6;           // 6 puntos = 3 corazones
        vida = vidaMaxima;
        monedas = 0;

        atacando = false;
        invencible = false;

        inventario.reiniciar();
        inventario.agregar("Espada");  // el héroe empieza armado
    }

    // ------------------------------------------------------------------
    // UN FRAME EN LA VIDA DEL HÉROE
    // ------------------------------------------------------------------
    @Override
    public void update() {

        if (atacando) {
            actualizarAtaque();   // mientras ataca, no camina
        } else {
            actualizarMovimiento();

            // Banderas de un solo uso: las leemos y las apagamos.
            if (teclado.ataque) {
                teclado.ataque = false;
                if (inventario.tiene("Espada")) {
                    atacando = true;
                    contadorAtaque = 0;
                }
            }
            if (teclado.accion) {
                teclado.accion = false;
                interactuar();
            }
        }

        revisarDanioDeEnemigos();
        actualizarInvencibilidad();
        revisarCambioDePantalla();

        // Sin vida → la máquina de estados salta a la pantalla de derrota.
        if (vida <= 0) {
            gp.estado = GamePanel.ESTADO_GAME_OVER;
        }
    }

    /** Movimiento con colisiones. El orden es la clave:
     *  1) elegir dirección  2) preguntar a CollisionChecker
     *  3) moverse SOLO si nada lo impide. */
    private void actualizarMovimiento() {

        boolean moviendose = teclado.arriba || teclado.abajo
                || teclado.izquierda || teclado.derecha;

        if (!moviendose) {
            // Quieto: reiniciamos la animación para que no "marche en el sitio".
            pasoAlterno = false;
            contadorAnimacion = 0;
            return;
        }

        // Una dirección a la vez (como en los clásicos): la primera
        // condición verdadera gana. Sin diagonales, sin complicaciones.
        if (teclado.arriba)         direccion = "arriba";
        else if (teclado.abajo)     direccion = "abajo";
        else if (teclado.izquierda) direccion = "izquierda";
        else                        direccion = "derecha";

        // Preguntamos al detector de colisiones ANTES de movernos.
        colisionDetectada = false;
        gp.colisiones.revisarTile(this);                    // ¿muro/agua/árbol?
        int indiceObjeto = gp.colisiones.revisarObjetos(this);  // ¿cofre/puerta?
        gp.colisiones.revisarEntidades(this, gp.npcs);      // ¿un NPC delante?

        if (!colisionDetectada) {
            moverSegunDireccion();
        }
        animar();

        // Si pisamos un objeto recogible (moneda, corazón...), lo tomamos.
        if (indiceObjeto != -1 && gp.objetos.get(indiceObjeto).recogible) {
            recogerObjeto(indiceObjeto);
        }
    }

    // ------------------------------------------------------------------
    // COMBATE CON ESPADA
    // El golpe es un rectángulo invisible delante del héroe. Cualquier
    // enemigo cuyo hitbox se cruce con él recibe daño. Su propia
    // invencibilidad temporal evita que un solo golpe le pegue 16 veces.
    // ------------------------------------------------------------------
    private void actualizarAtaque() {

        contadorAtaque++;
        Rectangle areaEspada = calcularAreaEspada();

        for (Enemy enemigo : gp.enemigos) {
            if (enemigo.pantalla == gp.mapa.pantallaActual
                    && !enemigo.invencible
                    && areaEspada.intersects(enemigo.areaMundo())) {
                enemigo.recibirDanio(1);
            }
        }

        if (contadorAtaque > DURACION_ATAQUE) {
            atacando = false;
            contadorAtaque = 0;
        }
    }

    /** Rectángulo de golpe según la dirección de la mirada. */
    private Rectangle calcularAreaEspada() {
        switch (direccion) {
            case "arriba":    return new Rectangle(x + 8, y - 24, 32, 28);
            case "abajo":     return new Rectangle(x + 8, y + 44, 32, 28);
            case "izquierda": return new Rectangle(x - 24, y + 8, 28, 32);
            default:          return new Rectangle(x + 44, y + 8, 28, 32);
        }
    }

    // ------------------------------------------------------------------
    // INTERACCIÓN (tecla ENTER)
    // Calculamos un punto UN TILE por delante del héroe y buscamos qué
    // hay ahí: primero NPCs, luego objetos del escenario.
    // ------------------------------------------------------------------
    private void interactuar() {

        int t = GamePanel.TAMANO_TILE;
        int objetivoX = x + t / 2;   // centro del héroe...
        int objetivoY = y + t / 2;

        switch (direccion) {         // ...desplazado un tile hacia delante
            case "arriba":    objetivoY -= t; break;
            case "abajo":     objetivoY += t; break;
            case "izquierda": objetivoX -= t; break;
            case "derecha":   objetivoX += t; break;
        }

        // ¿Hay un NPC en ese punto?
        for (NPC npc : gp.npcs) {
            if (npc.pantalla == gp.mapa.pantallaActual
                    && new Rectangle(npc.x, npc.y, t, t).contains(objetivoX, objetivoY)) {
                npc.hablar();   // el NPC abre el diálogo (estado DIALOGO)
                return;
            }
        }

        // ¿Hay un objeto interactuable? Bucle con índice porque podemos
        // borrar de la lista (la puerta desaparece al abrirse).
        for (int i = 0; i < gp.objetos.size(); i++) {
            GameObject objeto = gp.objetos.get(i);

            if (objeto.pantalla != gp.mapa.pantallaActual) continue;
            if (!new Rectangle(objeto.x, objeto.y, t, t).contains(objetivoX, objetivoY)) continue;

            switch (objeto.tipo) {

                case GameObject.CARTEL:
                    gp.ui.iniciarDialogo(new String[]{ objeto.texto });
                    return;

                case GameObject.COFRE:
                    if (!objeto.abierto) {
                        objeto.abierto = true;
                        gp.objetosRecogidos.add(objeto.id);  // para el guardado
                        inventario.agregar(objeto.contenido);
                        gp.ui.mostrarMensaje("¡Has encontrado: " + objeto.contenido + "!");
                    } else {
                        gp.ui.mostrarMensaje("El cofre está vacío.");
                    }
                    return;

                case GameObject.PUERTA:
                    if (inventario.tiene("Llave")) {
                        inventario.quitarUno("Llave");       // la llave se consume
                        gp.objetosRecogidos.add(objeto.id);
                        gp.objetos.remove(i);                // la puerta desaparece
                        gp.ui.mostrarMensaje("La puerta se abre.");
                    } else {
                        gp.ui.mostrarMensaje("Cerrada. Necesitas una llave.");
                    }
                    return;

                case GameObject.PALANCA:
                    if (!objeto.activada) {
                        objeto.activada = true;
                        gp.objetosRecogidos.add(objeto.id);
                        gp.eliminarObjetoPorId("puerta_jefe");  // ¡el acertijo!
                        gp.ui.mostrarMensaje("Algo se abre a lo lejos...");
                    }
                    return;
            }
        }
    }

    /** Recogida por contacto: corazones, monedas, llaves, pociones... */
    private void recogerObjeto(int indice) {

        GameObject objeto = gp.objetos.get(indice);

        switch (objeto.tipo) {
            case GameObject.CORAZON:
                vida = Math.min(vida + 2, vidaMaxima);  // nunca pasar del máximo
                gp.ui.mostrarMensaje("¡Recuperas un corazón!");
                break;
            case GameObject.MONEDA:
                monedas++;
                gp.ui.mostrarMensaje("Moneda obtenida");
                break;
            case GameObject.LLAVE:
                inventario.agregar("Llave");
                gp.ui.mostrarMensaje("¡Has encontrado una llave!");
                break;
            case GameObject.POCION:
                inventario.agregar("Poción");
                gp.ui.mostrarMensaje("Poción guardada en el inventario");
                break;
            case GameObject.RELIQUIA:
                inventario.agregar("Reliquia");
                gp.estado = GamePanel.ESTADO_VICTORIA;  // ¡fin del juego!
                break;
            default:
                return;  // no era recogible
        }

        gp.objetosRecogidos.add(objeto.id);
        gp.objetos.remove(indice);
    }

    /** Daño por contacto con enemigos (el clásico "roce" que quita vida). */
    private void revisarDanioDeEnemigos() {
        for (Enemy enemigo : gp.enemigos) {
            if (enemigo.pantalla == gp.mapa.pantallaActual
                    && enemigo.areaMundo().intersects(areaMundo())) {
                recibirDanio(1);  // respeta la invencibilidad temporal
            }
        }
    }

    /** Mundo dividido en pantallas: al cruzar un borde, GameMap nos
     *  teletransporta a la pantalla vecina y nos recoloca en el borde opuesto. */
    private void revisarCambioDePantalla() {
        int medio = GamePanel.TAMANO_TILE / 2;
        if (x < -medio)                      gp.mapa.transicion("oeste");
        else if (x > GamePanel.ANCHO - medio) gp.mapa.transicion("este");
        else if (y < -medio)                  gp.mapa.transicion("norte");
        else if (y > GamePanel.ALTO - medio)  gp.mapa.transicion("sur");
    }

    /** Dibujado: el héroe (lo hace Entity) + la espada si está atacando. */
    @Override
    public void draw(Graphics2D g2) {
        super.draw(g2);
        if (atacando) {
            dibujarEspada(g2);
        }
    }

    /** La espada se dibuja con rectángulos simples (hoja + guarda):
     *  blanco con borde negro, fiel a la estética del juego. */
    private void dibujarEspada(Graphics2D g2) {

        int t = GamePanel.TAMANO_TILE;
        int hojaX, hojaY, hojaAncho, hojaAlto;
        int guardaX, guardaY, guardaAncho, guardaAlto;

        switch (direccion) {
            case "arriba":
                hojaX = x + t / 2 - 4;  hojaY = y - 22;  hojaAncho = 8;  hojaAlto = 26;
                guardaX = x + t / 2 - 10; guardaY = y - 2; guardaAncho = 20; guardaAlto = 6;
                break;
            case "abajo":
                hojaX = x + t / 2 - 4;  hojaY = y + t - 4; hojaAncho = 8;  hojaAlto = 26;
                guardaX = x + t / 2 - 10; guardaY = y + t - 6; guardaAncho = 20; guardaAlto = 6;
                break;
            case "izquierda":
                hojaX = x - 22; hojaY = y + t / 2 - 4; hojaAncho = 26; hojaAlto = 8;
                guardaX = x - 4; guardaY = y + t / 2 - 10; guardaAncho = 6; guardaAlto = 20;
                break;
            default:  // derecha
                hojaX = x + t - 4; hojaY = y + t / 2 - 4; hojaAncho = 26; hojaAlto = 8;
                guardaX = x + t - 2; guardaY = y + t / 2 - 10; guardaAncho = 6; guardaAlto = 20;
                break;
        }

        g2.setColor(Color.WHITE);
        g2.fillRect(hojaX, hojaY, hojaAncho, hojaAlto);
        g2.fillRect(guardaX, guardaY, guardaAncho, guardaAlto);
        g2.setColor(Color.BLACK);
        g2.drawRect(hojaX, hojaY, hojaAncho, hojaAlto);
        g2.drawRect(guardaX, guardaY, guardaAncho, guardaAlto);
    }

    // ------------------------------------------------------------------
    // SPRITES DEL HÉROE (matrices de 16x16 caracteres)
    // Solo dibujamos tres: abajo, arriba y derecha. El de la izquierda
    // se genera reflejando el de la derecha con crearEspejo().
    // ------------------------------------------------------------------
    private void cargarSprites() {

        String[] patronAbajo = {
            "................",
            ".....333333.....",
            "....33333333....",
            "....31111113....",
            "....30000003....",
            "....30300303....",
            "....30000003....",
            ".....300003.....",
            "...3222222223...",
            "..312222222213..",
            "..312222222213..",
            "...3222222223...",
            "....33333333....",
            "....322..223....",
            "....322..223....",
            "....33....33...."
        };

        String[] patronArriba = {
            "................",
            ".....333333.....",
            "....33333333....",
            "....31111113....",
            "....31111113....",
            "....31111113....",
            "....31111113....",
            ".....311113.....",
            "...3222322223...",
            "..312223222213..",
            "..312223222213..",
            "...3222322223...",
            "....33333333....",
            "....322..223....",
            "....322..223....",
            "....33....33...."
        };

        String[] patronDerecha = {
            "................",
            ".....333333.....",
            "....33333333....",
            "....31111113....",
            "....31100003....",
            "....31100303....",
            "....31100003....",
            ".....310003.....",
            "...3222222223...",
            "...3221111223...",
            "...3221111223...",
            "...3222222223...",
            "....33333333....",
            ".....322223.....",
            ".....322223.....",
            ".....33..33....."
        };

        spriteAbajo = crearSprite(patronAbajo);
        spriteArriba = crearSprite(patronArriba);
        spriteDerecha = crearSprite(patronDerecha);
        spriteIzquierda = crearEspejo(spriteDerecha);  // reflejo gratuito
    }
}