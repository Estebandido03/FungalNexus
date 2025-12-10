package org.fungalnexus;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.input.MouseEvent;

import java.util.*;

public class PanelJuegoFX extends Pane {

    private final GrafoColonia grafoColonia;
    private final ConstruccionManager construccionManager;
    private final Circle ghostPreview; // El objeto visual fantasma

    private final Map<Nodo, Circle> nodoToCircleMap = new HashMap<>();
    private final Map<Nodo, List<Line>> nodoToHifasMap = new HashMap<>();

    public PanelJuegoFX(GrafoColonia grafoColonia, ConstruccionManager construccionManager) {
        this.grafoColonia = grafoColonia;
        this.construccionManager = construccionManager;

        // Configuración inicial del Pane
        this.setPrefSize(Configuracion.MAPA_WIDTH, Configuracion.MAPA_HEIGHT);
        this.setStyle("-fx-background-color: #333333;"); // Fondo oscuro para el micelio

        // Inicializar el Ghost Preview (se oculta hasta que se active el modo)
        this.ghostPreview = crearGhostPreview();
        this.getChildren().add(ghostPreview);

        // --- Manejo de Eventos del Ratón ---
        this.setOnMouseMoved(this::handleMouseMoved);
        this.setOnMouseClicked(this::handleMouseClicked);

        // Dibujar el estado inicial (solo el núcleo)
        dibujarColoniaInicial();
    }

    private Circle crearGhostPreview() {
        Circle circle = new Circle(Configuracion.NODO_RADIO);
        circle.setFill(Color.WHITE.deriveColor(0, 1.0, 1.0, 0.3)); // Color blanco semi-transparente
        circle.setStroke(Color.WHITE);
        circle.setVisible(false);
        return circle;
    }

    // --- Manejo del Movimiento del Ratón (GHOST PREVIEW) ---
    private void handleMouseMoved(MouseEvent event) {
        if (construccionManager.estaEnModoConstruccion()) {

            // 1. Determinar la posición del grid más cercana
            int xGrid = ((int) event.getX() / Configuracion.GRID_SIZE) * Configuracion.GRID_SIZE + Configuracion.GRID_SIZE / 2;
            int yGrid = ((int) event.getY() / Configuracion.GRID_SIZE) * Configuracion.GRID_SIZE + Configuracion.GRID_SIZE / 2;

            // 2. Mover el fantasma a la posición del grid
            ghostPreview.setCenterX(xGrid);
            ghostPreview.setCenterY(yGrid);
            ghostPreview.setVisible(true);

            // Actualizar el color del fantasma si no hay suficientes nutrientes
            double costo = construccionManager.getCostoSeleccionado();

            if (grafoColonia.getNutrientesTotales() < costo) {
                // No hay recursos: cambiar el color a rojo
                ghostPreview.setStroke(Color.RED);
                ghostPreview.setFill(Color.RED.deriveColor(0, 1.0, 1.0, 0.3));
            } else {
                // Recursos suficientes: mantener el color blanco por defecto
                ghostPreview.setStroke(Color.PALEGREEN);
                ghostPreview.setFill(Color.PALEGREEN.deriveColor(0, 1.0, 1.0, 0.3));
            }

        } else {
            ghostPreview.setVisible(false);
        }
    }

    // --- Manejo del Clic (CONSTRUCCIÓN) ---
    private void handleMouseClicked(MouseEvent event) {
        if (!construccionManager.estaEnModoConstruccion()) {
            return;
        }

        // 1. Posición final del grid
        int xGrid = ((int) event.getX() / Configuracion.GRID_SIZE) * Configuracion.GRID_SIZE + Configuracion.GRID_SIZE / 2;
        int yGrid = ((int) event.getY() / Configuracion.GRID_SIZE) * Configuracion.GRID_SIZE + Configuracion.GRID_SIZE / 2;

        TipoNodo tipo = construccionManager.getNodoSeleccionado();
        Nodo nuevoNodo = grafoColonia.construirNuevoNodo(xGrid, yGrid, tipo);

        if (nuevoNodo != null) {
            dibujarNuevoNodo(nuevoNodo);
        } else {
            // Notificar al usuario que no hay recursos
            System.out.println("No se puede construir. Nutrientes insuficientes.");
        }
    }

    // --- Métodos de Dibujo ---

    private void dibujarColoniaInicial() {
        Nodo nucleo = grafoColonia.getNucleo();

        // **IMPORTANTE**: Usar NODO_RADIO para que la lógica de transformación funcione bien
        Circle nucleoFX = new Circle(nucleo.getX(), nucleo.getY(), Configuracion.NUCLEO_RADIO, obtenerColor(nucleo.getTipo()));
        nucleoFX.setUserData(nucleo);

        this.getChildren().add(nucleoFX);

        // Almacenar el núcleo para que sea actualizado
        nodoToCircleMap.put(nucleo, nucleoFX);
    }

    private void dibujarNuevoNodo(Nodo nuevoNodo) {
        // 1. Dibujar el Nodo
        Circle nodoFX = new Circle(nuevoNodo.getX(), nuevoNodo.getY(), Configuracion.NODO_RADIO, obtenerColor(nuevoNodo.getTipo()));
        nodoFX.setUserData(nuevoNodo);
        this.getChildren().add(nodoFX);
        nodoToCircleMap.put(nuevoNodo, nodoFX);

        // 2. Dibujar la Hifa (conexión)
        // El nuevo nodo debe tener exactamente un vecino (su padre) recién conectado
        Nodo padre = nuevoNodo.getVecinos().get(0);

        Line hifaFX = new Line(nuevoNodo.getX(), nuevoNodo.getY(), padre.getX(), padre.getY());
        hifaFX.setStroke(Color.GREEN);
        hifaFX.setStrokeWidth(2.0);

        nodoToHifasMap.computeIfAbsent(nuevoNodo, k -> new ArrayList<>()).add(hifaFX);

        nodoToHifasMap.computeIfAbsent(padre, k -> new ArrayList<>()).add(hifaFX);

        // Añadir la hifa *debajo* del nodo para que no lo cubra
        this.getChildren().add(0, hifaFX);
    }

    private Color obtenerColor(TipoNodo tipo) {
        return switch (tipo) {
            case NUCLEO -> Color.PURPLE;
            case EXTRACTOR -> Color.YELLOWGREEN;
            case ALMACENAMIENTO -> Color.BLUE;
            case DEFENSA -> Color.SKYBLUE;
        };
    }

    public void actualizarVista() {

        // Iteramos sobre el mapa de rastreo existente, sin modificarlo.
        for (Map.Entry<Nodo, Circle> entry : nodoToCircleMap.entrySet()) {
            Nodo nodo = entry.getKey();
            Circle circle = entry.getValue();

            // 1. Definir los colores base y de infección
            Color baseColor = obtenerColor(nodo.getTipo());
            Color colorBacteria = Color.rgb(100, 0, 0); // Rojo oscuro/marrón
            Color colorFinal;

            // 2. Lógica de Transformación y Estado
            if (nodo.esBacteriaCompletada()) {

                // Si la transformación es completa (Salud <= 0)
                if (nodo.getTipo() == TipoNodo.NUCLEO) {
                    // Estado final de Derrota: Núcleo Bacteria
                    colorFinal = Color.DARKRED; // Rojo muy oscuro
                    circle.setRadius(Configuracion.NODO_RADIO * 2.0); // Visual de Game Over
                } else {
                    // Nodo de expansión transformado
                    colorFinal = Color.BLACK;
                    circle.setRadius(Configuracion.NODO_RADIO * 1.5);
                }

            } else {
                // El nodo está VIVO o en proceso de INFECCIÓN/TRANSFORMACIÓN

                double nivelInfeccion = nodo.getNivelInfeccion();
                double saludRelativa = nodo.getSalud() / nodo.getTipo().getSaludBase();

                // A. Interpolar entre el color base y el color bacteria
                Color colorIntermedio = baseColor.interpolate(colorBacteria, nivelInfeccion);

                // B. Oscurecer el resultado según la salud (feedback de daño)
                colorFinal = colorIntermedio.darker().interpolate(colorIntermedio, saludRelativa);

                circle.setRadius(Configuracion.NODO_RADIO); // Restaurar tamaño normal

                // Opcional: Si el nodo está infectado (nivelInfeccion > 0),
                // haz un pequeño pulso visual para alertar al jugador
                if (nivelInfeccion > 0) {
                    circle.setOpacity(0.6 + Math.abs(Math.sin(System.currentTimeMillis() / 100.0)) * 0.4);
                } else {
                    circle.setOpacity(1.0);
                }
            }

            circle.setFill(colorFinal);
        }
    }
}