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