package org.fungalnexus;

import java.util.*;

public class ColoniaGrafo {

    // Lista de adyacencia: Mapea el nombre de un centro a la lista de hifas salientes.
    private Map<String, List<Hifa>> adj = new HashMap<>();
    private Map<String, Centro> centros = new HashMap<>();

    // --- 1. Gestión de Centros y Nodos ---

    public void agregarCentro(Centro centro) {
        centros.put(centro.getNombre(), centro);
        adj.putIfAbsent(centro.getNombre(), new ArrayList<>());
    }

    public Centro obtenerCentro(String nombre) {
        return centros.get(nombre);
    }

    public List<Centro> obtenerTodosLosCentros() {
        return new ArrayList<>(centros.values());
    }

    public Set<String> obtenerNombresDeCentros() {
        return centros.keySet();
    }

    public int calcularProduccionTotal(String recurso) {
        int produccionTotal = 0;

        // Iterar sobre todos los centros conocidos
        for (Centro centro : centros.values()) {

            // Solo los centros de tipo "Extraccion" producen recursos
            if (centro.getTipo().equals("Extraccion")) {
                produccionTotal += centro.getTasaProduccion(recurso);
            }
        }
        return produccionTotal;
    }

    public int calcularCostoTotalMantenimiento(String recurso) {
        int costoTotal = 0;

        // 1. COSTO OPERACIONAL LOCAL (De cada Centro)
        // Sumamos el costo fijo de operación (Ej: 2 unidades para cada Extractor)
        for (Centro centro : centros.values()) {
            costoTotal += centro.getCostoOperacional(recurso);
        }

        // 2. COSTO DE MANTENIMIENTO DE LAS HIFAS (Conexiones)
        // Iteramos sobre todos los conjuntos de hifas salientes en el grafo (`adj` values).
        for (List<Hifa> hifasOrigen : adj.values()) {
            for (Hifa hifa : hifasOrigen) {
                // Sumamos el costoMantenimiento de cada Hifa
                // Asumimos que es el costo en Nutrientes, ya que Hifa solo tiene un costo.
                costoTotal += hifa.getCostoMantenimiento();
            }
        }

        return costoTotal;
    }

    // --- 2. Gestión de Hifas (Aristas) ---

    public void conectarCentros(String origen, String destino, int capacidad, int costoMantenimiento, int salud) {
        // Validación básica
        if (!centros.containsKey(origen) || !centros.containsKey(destino)) {
            System.err.println("Error: El origen o el destino no existen en el grafo.");
            return;
        }

        Hifa hifa = new Hifa(destino, capacidad, costoMantenimiento);
        // La salud de la Hifa se establece en el constructor de Hifa, no es necesaria aquí.
        adj.get(origen).add(hifa);
    }

    public List<Hifa> obtenerHifas(String origen) {
        return adj.getOrDefault(origen, Collections.emptyList());
    }

    public List<List<Hifa>> obtenerTodasLasHifas() {
        return new ArrayList<>(adj.values());
    }
}