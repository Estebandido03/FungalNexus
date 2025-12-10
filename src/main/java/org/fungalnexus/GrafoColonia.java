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

    // --- Estado de la Amenaza ---
    private final List<Nodo> nodosInfectados;

    private boolean gameOver = false;

    // --- Constructor ---
    public GrafoColonia(int nucleoX, int nucleoY) {
        this.nodosColonia = new HashSet<>();
        this.nodosInfectados = new ArrayList<>();

        // Inicialización del Núcleo (asumimos tasa de producción 0, ya que solo almacena)
        this.nucleo = new Nodo(nucleoX, nucleoY, TipoNodo.NUCLEO, TipoNodo.NUCLEO.getTasaProduccion());

        // Inicialización de recursos y capacidad
        this.nutrientesTotales = 450;
        this.defensasTotales = 0.0;

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
        double produccionDefensaNeta = 0.0;

        for (Nodo nodo : nodosColonia) {
            if (nodo.getSalud() > 0) { // Solo si el nodo está vivo
                if (nodo.getTipo() == TipoNodo.EXTRACTOR) {
                    // Sumar la extracción
                    extraccionNeta += nodo.getTasaProduccion();
                } else if (nodo.getTipo() == TipoNodo.DEFENSA) {
                    // Sumar la producción de defensas
                    produccionDefensaNeta += nodo.getTasaProduccion();
                }
            }
        }

        // Aplicar la extracción hasta el límite de capacidad
        double nuevoTotal = this.nutrientesTotales + extraccionNeta;
        this.nutrientesTotales = Math.min(nuevoTotal, this.capacidadNutrienteTotal);

        // Las defensas no deberían tener un límite estricto de almacenamiento, solo de uso.
        this.defensasTotales += produccionDefensaNeta;
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
                nodoInfectado.setNivelInfeccion(Math.max(0, nodoInfectado.getNivelInfeccion() - 0.1)); // Reduce infección
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
            // Podríamos decidir infectar el núcleo si el tiempo de gracia se agota mucho,
            // pero por ahora, solo espera.
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