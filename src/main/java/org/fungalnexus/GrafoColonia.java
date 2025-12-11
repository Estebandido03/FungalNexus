package org.fungalnexus;

import java.util.*;

public class GrafoColonia {

    // --- Estructura del Grafo ---
    // Mantiene la lista de todos los nodos para fácil iteración
    private final Set<Nodo> nodosColonia;
    private final Nodo nucleo; // Referencia directa y constante al nodo central

    // --- Recursos Globales del Juego ---
    private double nutrientesTotales;
    private double defensasTotales;

    // Capacidad: Se recalcula cada vez que se construye o destruye un nodo ALMACENAMIENTO.
    private double capacidadNutrienteTotal;
    private double LIMITE_DEFENSA = 100.0;

    // --- Estado de la Amenaza ---
    private final List<Nodo> nodosInfectados;

    private PanelJuegoFX panelJuegoFX;

    private boolean gameOver = false;

    // --- Constructor ---
    public GrafoColonia(int nucleoX, int nucleoY, PanelJuegoFX panelJuegoFX) {
        this.nodosColonia = new HashSet<>();
        this.nodosInfectados = new ArrayList<>();

        // Inicialización del Núcleo (asumimos tasa de producción 0, ya que solo almacena)
        this.nucleo = new Nodo(nucleoX, nucleoY, TipoNodo.NUCLEO, TipoNodo.NUCLEO.getTasaProduccion());

        // Inicialización de recursos y capacidad
        this.nutrientesTotales = 450;
        this.defensasTotales = 0.0;

        this.panelJuegoFX = panelJuegoFX;

        // El núcleo proporciona la capacidad base
        this.capacidadNutrienteTotal = TipoNodo.NUCLEO.getCapacidadNutriente();

        // Agregar el núcleo al conjunto de nodos
        agregarNodo(nucleo);
    }

    public double getNutrientesTotales() {
        return nutrientesTotales;
    }

    public double getCapacidadNutrienteTotal() {
        return capacidadNutrienteTotal;
    }

    public double getDefensasTotales() {
        return defensasTotales;
    }

    public void setPanelJuegoFX(PanelJuegoFX panelJuegoFX) {
        this.panelJuegoFX = panelJuegoFX;
    }

    public void agregarNodo(Nodo nodo) {
        nodosColonia.add(nodo);

        // Si es un nodo de Almacenamiento, actualiza la capacidad global
        if (nodo.getTipo() == TipoNodo.ALMACENAMIENTO) {
            recalcularCapacidadTotal();
        }
    }

    //Conecta dos nodos (A y B) para crear una hifa bidireccional
    public void conectarNodos(Nodo nodoA, Nodo nodoB) {
        if (nodoA != null && nodoB != null && !nodoA.getVecinos().contains(nodoB)) {
            nodoA.agregarVecino(nodoB);
            nodoB.agregarVecino(nodoA);
        }
    }

    //Lógica central para la construcción del nuevo nodo en el juego. Devuelve el nuevo nodo o null si falló (ej. sin nutrientes).
    public Nodo construirNuevoNodo(int x, int y, TipoNodo tipo) {

        double costoNutrientes = tipo.getCosto();
        double tasa = (tipo == TipoNodo.EXTRACTOR) ? tipo.getTasaProduccion() : tipo.getTasaDefensa();

        if (this.nutrientesTotales < costoNutrientes) {
            return null;
        }

        Nodo nodoPadre = encontrarNodoMasCercano(x, y);
        if (nodoPadre == null) {
            return null;
        }

        Nodo nuevoNodo = new Nodo(x, y, tipo, tasa);

        this.nutrientesTotales -= costoNutrientes;
        agregarNodo(nuevoNodo);
        conectarNodos(nodoPadre, nuevoNodo);

        return nuevoNodo;
    }

    //Reimplementacion del metodo auxiliar del nodo mas cercano
    public Nodo encontrarNodoMasCercano(int xNuevo, int yNuevo) {
        Nodo masCercano = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Nodo nodoExistente : nodosColonia) {
            double distancia = Math.sqrt(Math.pow(xNuevo - nodoExistente.getX(), 2) + Math.pow(yNuevo - nodoExistente.getY(), 2));

            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                masCercano = nodoExistente;
            }
        }
        return masCercano;
    }

    public List<Nodo> encontrarRutaDesdeTipo(Nodo destino, TipoNodo tipoBuscado) {
        Map<Nodo, Nodo> padres = new HashMap<>();
        Queue<Nodo> cola = new LinkedList<>();
        Set<Nodo> visitados = new HashSet<>();
        Nodo nodoOrigenEncontrado = null;

        // Empezamos la búsqueda desde el destino (el nodo infectado)
        cola.add(destino);
        visitados.add(destino);

        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();

            // Condición de parada: Encontrar el nodo DEFENSA más cercano y activo
            if (actual.getTipo() == tipoBuscado && actual.getSalud() > 0) {
                nodoOrigenEncontrado = actual;
                break;
            }

            // Si es el núcleo, podría ser una fuente de emergencia, pero
            // por ahora solo buscamos nodos DEFENSA.

            for (Nodo vecino : actual.getVecinos()) {
                if (!visitados.contains(vecino)) {
                    visitados.add(vecino);
                    padres.put(vecino, actual);
                    cola.add(vecino);
                }
            }
        }

        if (nodoOrigenEncontrado == null) return null;

        // Reconstruir la ruta al revés
        List<Nodo> ruta = new LinkedList<>();
        Nodo paso = destino;
        while (paso != null && padres.containsKey(paso)) {
            ruta.add(0, paso); // Agrega al inicio
            paso = padres.get(paso);
        }
        // Añadir el nodo DEFENSA encontrado como punto de partida
        ruta.add(0, nodoOrigenEncontrado);

        return ruta;
    }

    /**
     * Encuentra la ruta más corta desde el nodo inicial hasta el Núcleo usando BFS.
     * @param inicio El nodo extractor de origen.
     * @return Una lista de nodos que forman la ruta, incluyendo el inicio y el núcleo, o null si no hay ruta.
     */
    public List<Nodo> encontrarRutaAlNucleo(Nodo inicio) {
        if (inicio == nucleo) return List.of(nucleo);

        // Almacena el nodo previo en la ruta para reconstruirla
        Map<Nodo, Nodo> padres = new HashMap<>();
        Queue<Nodo> cola = new LinkedList<>();
        Set<Nodo> visitados = new HashSet<>();

        cola.add(inicio);
        visitados.add(inicio);

        while (!cola.isEmpty()) {
            Nodo actual = cola.poll();

            if (actual == nucleo) {
                // Reconstruir y retornar la ruta
                List<Nodo> ruta = new LinkedList<>();
                Nodo paso = nucleo;
                while (paso != null) {
                    ruta.add(0, paso); // Añadir al inicio para invertir la ruta
                    paso = padres.get(paso);
                }
                return ruta;
            }

            for (Nodo vecino : actual.getVecinos()) {
                if (!visitados.contains(vecino)) {
                    visitados.add(vecino);
                    padres.put(vecino, actual);
                    cola.add(vecino);
                }
            }
        }
        return null; // No se encontró ruta
    }

    // Recalcula la capacidad de almacenamiento global sumando la capacidad de todos los nodos.
    private void recalcularCapacidadTotal() {
        this.capacidadNutrienteTotal = 0;
        for (Nodo nodo : nodosColonia) {
            if (nodo.getTipo() == TipoNodo.NUCLEO || nodo.getTipo() == TipoNodo.ALMACENAMIENTO) {
                this.capacidadNutrienteTotal += nodo.getTipo().getCapacidadNutriente();
            }
        }
    }

    public void actualizarRecursos() {
        double extraccionNeta = 0.0;
        double produccionDefensaNeta = 0.0; // Lo mantenemos por claridad

        for (Nodo nodo : nodosColonia) {
            if (nodo.getSalud() > 0) {
                if (nodo.getTipo() == TipoNodo.EXTRACTOR) {
                    extraccionNeta += nodo.getTasaProduccion();


                    if (panelJuegoFX != null) {

                        List<Nodo> rutaRecorrido = encontrarRutaAlNucleo(nodo);
                        // El destino será el núcleo
                        if (rutaRecorrido != null && rutaRecorrido.size() > 1) {
                            // pasamos la ruta completa
                            panelJuegoFX.crearParticula(
                                    nodo.getX(), nodo.getY(),
                                    rutaRecorrido, // Pasamos la ruta
                                    TipoRecurso.NUTRIENTE
                            );
                        }
                    }
                } else if (nodo.getTipo() == TipoNodo.DEFENSA) {
                    this.defensasTotales += nodo.getTasaDefensa();
                    produccionDefensaNeta += nodo.getTasaProduccion(); // Asumiendo que getTasaProduccion es la tasa de defensa
                }
            }
        }

        // APLICAR LÍMITE GLOBAL
        double nuevoTotal = this.nutrientesTotales + extraccionNeta;
        this.nutrientesTotales = Math.min(nuevoTotal, this.capacidadNutrienteTotal);

        // Si hubo exceso, puedes imprimir un mensaje:
        if (nuevoTotal > this.capacidadNutrienteTotal) {
            System.out.println("Almacenamiento lleno. Exceso de " + (nuevoTotal - this.capacidadNutrienteTotal) + " perdido.");
        }

        double nuevoTotalDefensa = this.defensasTotales + produccionDefensaNeta;
        this.defensasTotales = Math.min(nuevoTotalDefensa, LIMITE_DEFENSA);

    }

    public void actualizarInfeccionYCombate(double factorPropagacion, double factorDano, double costoDefensa) {
        if (gameOver) return;
        // 1. PROPAGACIÓN: Identificar los nuevos nodos que se van a infectar
        Set<Nodo> nuevosInfectados = new HashSet<>();

        for (Nodo nodoInfectado : nodosInfectados) {
            // 1. Infligir daño al nodo (si la bacteria aún no completó la transformación)
            if (!nodoInfectado.esBacteriaCompletada()) {
                nodoInfectado.recibirDano(factorDano);
            }

            // 2. Intentar Sanar / Contener (solo si aún no está transformado)
            if (!nodoInfectado.esBacteriaCompletada() && this.defensasTotales >= costoDefensa) {
                this.defensasTotales -= costoDefensa;
                nodoInfectado.setNivelInfeccion(Math.max(0, nodoInfectado.getNivelInfeccion() - 0.1));
                // **GENERAR PARTÍCULA DE DEFENSA** (desde el Núcleo hacia el nodo infectado)
                if (!nodoInfectado.esBacteriaCompletada() && this.defensasTotales >= costoDefensa) {
                    this.defensasTotales -= costoDefensa;
                    nodoInfectado.setNivelInfeccion(Math.max(0, nodoInfectado.getNivelInfeccion() - 0.1));

                    // **GENERAR PARTÍCULA DE DEFENSA**
                    if (panelJuegoFX != null) {

                        // 1. Intentar encontrar la ruta desde el nodo DEFENSA más cercano
                        List<Nodo> rutaDefensa = encontrarRutaDesdeTipo(nodoInfectado, TipoNodo.DEFENSA);

                        // 2. Lógica de Respaldo: Si no se encuentra un nodo DEFENSA conectado
                        if (rutaDefensa == null || rutaDefensa.size() < 2) {

                            // a) Encontrar la ruta inversa al Núcleo: [Infectado, ..., Nucleo]
                            List<Nodo> rutaInversa = encontrarRutaAlNucleo(nodoInfectado);

                            if (rutaInversa != null && rutaInversa.size() > 1) {
                                // b) Invertir la ruta para que sea [Nucleo, ..., Infectado]
                                rutaDefensa = new ArrayList<>(rutaInversa); // Crear una copia mutable
                                Collections.reverse(rutaDefensa); // Necesita importación de java.util.Collections
                            } else {
                                // No hay conexión ni al Núcleo, no se puede enviar la partícula.
                                rutaDefensa = null;
                            }
                        }

                        // 3. Si finalmente tenemos una ruta válida (desde DEFENSA o NUCLEO)
                        if (rutaDefensa != null && rutaDefensa.size() > 1) {

                            // El origen de la partícula es el primer nodo de la ruta
                            Nodo origenParticula = rutaDefensa.get(0);

                            panelJuegoFX.crearParticula(
                                    origenParticula.getX(), origenParticula.getY(),
                                    rutaDefensa,
                                    TipoRecurso.DEFENSA
                            );
                        }
                    }
                }
            }

            // 3. Lógica de Transformación:
            if (nodoInfectado.getSalud() <= 0 && !nodoInfectado.esBacteriaCompletada()) {
                nodoInfectado.setEsBacteriaCompletada(true);
                nodoInfectado.setSalud(0);
                nodoInfectado.setNivelInfeccion(1.0);

                System.out.println("Nodo " + nodoInfectado.getTipo() + " se ha TRANSFORMADO en Nodo Bacteria (Salud 0).");
                if (nodoInfectado.getTipo() == TipoNodo.NUCLEO) {
                    System.out.println(">>> ¡DERROTA! El Núcleo se ha transformado en un Núcleo Bacterial. <<<");
                    this.gameOver = true;
                }
            }

            // 4. Propagación de la Bacteria
            // La propagación ocurre si el nodo está infectado Y si es una Bacteria COMPLETADA
            if (nodoInfectado.esBacteriaCompletada() || Math.random() < factorPropagacion) {

                for (Nodo vecino : nodoInfectado.getVecinos()) {
                    if (vecino.getNivelInfeccion() == 0 && !vecino.esBacteriaCompletada()) {
                        nuevosInfectados.add(vecino);
                    }
                }
            }
        }

        // 2. APLICAR NUEVAS INFECCIONES
        for (Nodo nuevoInfectado : nuevosInfectados) {
            if (!nodosInfectados.contains(nuevoInfectado)) {
                nuevoInfectado.setNivelInfeccion(0.1); // Refuerza el nivel de infección
                nodosInfectados.add(nuevoInfectado);
            }
        }

        // 3. VERIFICACIÓN DE FIN DE JUEGO
        verificarGameOver();

        nodosInfectados.removeIf(nodo -> nodo.getNivelInfeccion() <= 0 && !nodo.esBacteriaCompletada());
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void verificarGameOver() {
        if (nucleo.esBacteriaCompletada()) {
            this.gameOver = true;
        }
    }

    public void iniciarPrimeraInfeccion() {
        // Solo iniciamos si no hay infecciones activas
        if (!nodosInfectados.isEmpty()) {
            return;
        }

        // 1. Crear una lista de candidatos (todos los nodos EXCEPTO el núcleo)
        List<Nodo> candidatos = new ArrayList<>();
        for (Nodo nodo : nodosColonia) {
            if (nodo.getTipo() != TipoNodo.NUCLEO) {
                candidatos.add(nodo);
            }
        }

        // 2. Verificar si hay nodos para infectar
        if (candidatos.isEmpty()) {
            // La colonia aún no se ha expandido, la infección debe esperar.
            return;
        }

        // 3. Elegir un nodo de expansión aleatorio para el punto inicial de infección
        Random random = new Random();
        Nodo nodoInicial = candidatos.get(random.nextInt(candidatos.size()));

        System.out.println("--- ¡ALERTA! Infección PERIFÉRICA iniciada en nodo "
                + nodoInicial.getTipo() + " en ("
                + nodoInicial.getX() + ", " + nodoInicial.getY() + ") ---");

        // 4. Aplicar la infección y añadirlo a la lista de seguimiento
        nodoInicial.setNivelInfeccion(0.1);
        nodosInfectados.add(nodoInicial);
    }

    //Devuelve la lista actual de nodos que se encuentran infectados por bacterias.
    //Es crucial para la lógica de inicio de infección en el Game Loop.
    public List<Nodo> getNodosInfectados() {
        return nodosInfectados;
    }

    public Nodo getNucleo() {
        return nucleo;
    }
}