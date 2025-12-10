package org.fungalnexus;

import java.util.ArrayList;
import java.util.List;

public class Nodo {

    // --- 1. Propiedades del Grafo (Fijas) ---
    private final int x; // Coordenada X en el grid
    private final int y; // Coordenada Y en el grid
    private final TipoNodo tipo;
    private final List<Nodo> vecinos; // Lista de Nodos adyacentes (Hifas)

    // --- 2. Propiedades de la Jugabilidad (Variables) ---
    private double salud; // Puntos de salud actuales (Máx: TipoNodo.getSaludBase())
    private double nivelInfeccion; // 0.0 a 1.0 (cuán infectado está)
    private double recursosAlmacenados; // Para nodos de TipoNodo.ALMACENAMIENTO y NUCLEO
    private boolean esBacteriaCompletada = false;

    // Propiedades Específicas del Tipo
    private double tasaProduccion; // Tasa de extracción (si es EXTRACTOR) o tasa de defensa (si es DEFENSA)

    // --- Constructor ---
    public Nodo(int x, int y, TipoNodo tipo, double tasaProduccion) {
        this.x = x;
        this.y = y;
        this.tipo = tipo;
        this.vecinos = new ArrayList<>();

        // Inicialización basada en el Enum
        this.salud = tipo.getSaludBase();
        this.recursosAlmacenados = 0.0;
        this.nivelInfeccion = 0.0;
        this.tasaProduccion = tasaProduccion; // Puede variar dependiendo del nivel de upgrade
    }

    // --- Métodos del Grafo ---

    public void agregarVecino(Nodo vecino) {
        if (vecino != null && !vecinos.contains(vecino)) {
            vecinos.add(vecino);
        }
    }

    // Metodo para la transformación
    public void setEsBacteriaCompletada(boolean estado) {
        this.esBacteriaCompletada = estado;
        // Una vez transformado, pierde su función y capacidad de curación
        this.tasaProduccion = 0;
        // Opcional: Cambiar su tipo para la lógica futura, aunque la bandera es suficiente.
    }

    public boolean esBacteriaCompletada() {
        return esBacteriaCompletada;
    }

    // --- Getters Esenciales ---

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TipoNodo getTipo() {
        return tipo;
    }

    public List<Nodo> getVecinos() {
        return vecinos;
    }

    public double getSalud() {
        return salud;
    }

    public double getNivelInfeccion() {
        return nivelInfeccion;
    }

    public double getCapacidadMaxima() {
        return tipo.getCapacidadNutriente();
    }

    public double getTasaProduccion() {
        return tasaProduccion;
    }

    // --- Setters / Lógica del Juego ---

    public void recibirDano(double dano) {
        this.salud = Math.max(0, this.salud - dano);
    }

    public void setNivelInfeccion(double nivelInfeccion) {
        this.nivelInfeccion = Math.min(1.0, Math.max(0.0, nivelInfeccion)); // Asegura que esté entre 0 y 1
    }

    public void setSalud(double salud) {
        this.salud = salud;
    }

    // Metodo para manejar almacenamiento de recursos (solo si es almacenamiento o nucleo)
    public double almacenarNutrientes(double cantidad) {
        if (tipo == TipoNodo.NUCLEO || tipo == TipoNodo.ALMACENAMIENTO) {
            double capacidadDisponible = getCapacidadMaxima() - this.recursosAlmacenados;
            double aAlmacenar = Math.min(cantidad, capacidadDisponible);
            this.recursosAlmacenados += aAlmacenar;
            return cantidad - aAlmacenar; // Devuelve el sobrante
        }
        return cantidad; // Si no tiene capacidad, devuelve el sobrante
    }

    //Implementacion del metodo equals y hashCode es crucial para usar Mapas y Sets con nodos
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nodo nodo = (Nodo) o;
        // Dos nodos son iguales si están en la misma posición (asumiendo que solo hay un nodo por celda del grid)
        return x == nodo.x && y == nodo.y;
    }

    @Override
    public int hashCode() {
        // Hash basado en las coordenadas
        return 31 * x + y;
    }
}