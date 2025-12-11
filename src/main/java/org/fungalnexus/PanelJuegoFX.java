package org.fungalnexus;

import javafx.animation.KeyFrame;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.input.MouseEvent;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.util.Duration;

import org.fungalnexus.Nodo;
import org.fungalnexus.TipoNodo;

import java.util.*;

public class PanelJuegoFX extends Pane {

    private GrafoColonia grafoColonia;
    private final ConstruccionManager construccionManager;
    private final Circle ghostPreview; // El objeto visual fantasma

    private final Map<Nodo, ImageView> nodoToImageViewMap = new HashMap<>();
    private ImageView gameOverImageView;
    private final Map<Nodo, List<Line>> nodoToHifasMap = new HashMap<>();

    private final List<ParticulaRecurso> particulasActivas;

    private Label timerLabel;
    private Timeline timeline;
    private long tiempoTranscurridoSegundos = 0;

    public PanelJuegoFX(GrafoColonia grafoColonia, ConstruccionManager construccionManager) {
        this.grafoColonia = grafoColonia;
        this.construccionManager = construccionManager;
        this.particulasActivas = new ArrayList<>();

        // Configuración inicial del Pane
        this.setPrefSize(Configuracion.MAPA_WIDTH, Configuracion.MAPA_HEIGHT);
        this.setStyle("-fx-background-color: #333333;"); // Fondo oscuro para el micelio

        inicializarCronometro();

        // Inicializar el Ghost Preview (se oculta hasta que se active el modo)
        this.ghostPreview = crearGhostPreview();
        this.getChildren().add(ghostPreview);

        // --- Manejo de Eventos del Ratón ---
        this.setOnMouseMoved(this::handleMouseMoved);
        this.setOnMouseClicked(this::handleMouseClicked);

    }

    public void setGrafoColonia(GrafoColonia grafoColonia) {
        this.grafoColonia = grafoColonia;
        // Dibujar el estado inicial (solo el núcleo)
        dibujarColoniaInicial();
        iniciarCronometro();
    }

    private void inicializarCronometro() {
        timerLabel = new Label("Tiempo: 00:00");
        timerLabel.setTextFill(Color.WHITE); // Texto blanco (ajústalo si tu fondo no es oscuro)
        timerLabel.setFont(new Font("Arial", 20)); // Fuente y tamaño

        // Posicionar en la esquina superior izquierda
        timerLabel.setLayoutX(10);
        timerLabel.setLayoutY(10);

        this.getChildren().add(timerLabel);
    }

    public void iniciarCronometro() {
        tiempoTranscurridoSegundos = 0; // Reiniciar al inicio de la partida

        // Creamos la Timeline
        timeline = new Timeline(
                // KeyFrame: Define qué hacer cada vez que pasa un segundo (Duration.seconds(1))
                new KeyFrame(Duration.seconds(1), event -> {
                    tiempoTranscurridoSegundos++;
                    actualizarLabelCronometro();
                })
        );

        // La Timeline se repetirá indefinidamente
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void detenerCronometro() {
        if (timeline != null) {
            timeline.stop();
            System.out.println("Partida terminada. Tiempo total: " + timerLabel.getText());
        }
    }

    private void actualizarLabelCronometro() {
        long minutos = tiempoTranscurridoSegundos / 60;
        long segundos = tiempoTranscurridoSegundos % 60;

        // Formato MM:SS (ej. 05:03)
        String tiempoFormateado = String.format("%02d:%02d", minutos, segundos);
        timerLabel.setText("Tiempo: " + tiempoFormateado);
    }

    public void crearParticula(double startX, double startY, List<Nodo> ruta, TipoRecurso tipo) {
        if (ruta == null || ruta.size() < 2) return; // Asegurar que haya una ruta válida

        ParticulaRecurso particula = new ParticulaRecurso(startX, startY, ruta, tipo);
        particulasActivas.add(particula);
        this.getChildren().add(particula);
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

        String rutaImagen = obtenerRutaDeImagenPorTipo(nucleo);
        Image imagen = new Image(getClass().getResourceAsStream(rutaImagen));
        ImageView imageView = new ImageView(imagen);

        imageView.setX(nucleo.getX() - (nucleo.getRadio() * 1.5));
        imageView.setY(nucleo.getY() - (nucleo.getRadio() * 2.5));
        imageView.setFitWidth(nucleo.getRadio() * 10);
        imageView.setFitHeight(nucleo.getRadio() * 10);
        this.getChildren().add(imageView);

        nodoToImageViewMap.put(nucleo, imageView);
    }

    private void dibujarNuevoNodo(Nodo nuevoNodo) {

        String rutaImagen = obtenerRutaDeImagenPorTipo(nuevoNodo);

        Image imagen = new Image(
                getClass().getResourceAsStream(rutaImagen)
        );

        ImageView imageView = new ImageView(imagen);

        imageView.setX(nuevoNodo.getX() - (nuevoNodo.getRadio() * 1.5)); // Centrar la imagen
        imageView.setY(nuevoNodo.getY() - (nuevoNodo.getRadio() * 2.5));
        imageView.setFitWidth(nuevoNodo.getRadio() * 3);  // Ajustar tamaño
        imageView.setFitHeight(nuevoNodo.getRadio() * 3);
        this.getChildren().add(imageView);

        Nodo padre = nuevoNodo.getVecinos().get(0);
        this.nodoToImageViewMap.put(nuevoNodo, imageView);

        Line hifaFX = new Line(nuevoNodo.getX(), nuevoNodo.getY(), padre.getX(), padre.getY());
        hifaFX.setStroke(Color.TAN);
        hifaFX.setStrokeWidth(2.0);

        nodoToHifasMap.computeIfAbsent(nuevoNodo, k -> new ArrayList<>()).add(hifaFX);

        nodoToHifasMap.computeIfAbsent(padre, k -> new ArrayList<>()).add(hifaFX);

        // Añadir la hifa *debajo* del nodo para que no lo cubra
        this.getChildren().add(0, hifaFX);
    }

    private String obtenerRutaDeImagenPorTipo(Nodo nodo) {

        // 1. Verificar si el nodo ha sido completamente transformado en bacteria
        if (nodo.esBacteriaCompletada()) {

            // Retornar la imagen de la bacteria correspondiente al tipo original
            switch (nodo.getTipoOriginal()) { // Asume que tienes un getTipoOriginal()
                case EXTRACTOR:
                    return "/gmushroom_bug.png";
                case DEFENSA:
                    return "/bmushroom_bug.png";
                case ALMACENAMIENTO:
                    return "/ymushroom_bug.png";
                case NUCLEO:
                    return "/pmushroom_bug.png";
                default:
                    // Si el tipo transformado no es específico, usa una imagen genérica de Bacteria
                    return "/gmushroom_bug.png";
            }
        }

        // 2. Si no es una bacteria completada, retorna la imagen normal basada en el tipo actual
        switch (nodo.getTipo()) {
            case NUCLEO:
                return "/pmushroom.png";
            case EXTRACTOR:
                return "/gmushroom.png";
            case DEFENSA:
                return "/bmushroom.png";
            case ALMACENAMIENTO:
                return "/ymushroom.png";
            default:
                return "/gmushroom.png";
        }
    }


    public void mostrarPantallaGameOver() {

        // 1. Verificar si ya se inicializó
        if (gameOverImageView != null) {
            gameOverImageView.setVisible(true);
            return;
        }

        // 2. Cargar la imagen (Usando la ruta probada)
        String rutaImagen = "/gameover2.png";
        Image imagen;

        try {
            imagen = new Image(getClass().getResourceAsStream(rutaImagen));
        } catch (NullPointerException e) {
            System.err.println("Error al cargar la imagen de Game Over. Ruta no encontrada: " + rutaImagen);
            return;
        }

        // 3. Crear y configurar el ImageView
        gameOverImageView = new ImageView(imagen);

        // 4. Posicionar ligeramente hacia arriba y centrado horizontalmente

        // Calcular la posición X (Centro del Pane - Mitad del Ancho de la Imagen)
        double centroX = this.getPrefWidth() / 2.0;
        double anchoImagen = imagen.getWidth();
        gameOverImageView.setX(centroX - (anchoImagen / 2.0));

        // Posición Y: Ligeramente hacia arriba (ej. 20% del total de la altura)
        double alturaCanvas = this.getPrefHeight();
        gameOverImageView.setY(alturaCanvas * 0.20);


        gameOverImageView.setFitWidth(69);
        gameOverImageView.setFitHeight(42);

        // 5. Agregar a la escena y poner al frente
        this.getChildren().add(gameOverImageView);
        gameOverImageView.toFront();
    }

    public void actualizarVista() {

        // Iteramos sobre el mapa de rastreo existente, sin modificarlo.
        for (Map.Entry<Nodo, ImageView> entry : nodoToImageViewMap.entrySet()) {
            Nodo nodo = entry.getKey();
            ImageView imageView = entry.getValue();

            // 1. Obtener la ruta de imagen actualizada (cambia si el nodo muere o no)
            String nuevaRuta = obtenerRutaDeImagenPorTipo(nodo);

            // **A. Actualización de Imagen (Si el estado cambia)**
            // (Podrías optimizar esto para solo cargar si la ruta ha cambiado, pero por ahora es simple)
            Image nuevaImagen = new Image(getClass().getResourceAsStream(nuevaRuta));
            imageView.setImage(nuevaImagen);

            // 2. Lógica de Transformación y Estado
            if (nodo.esBacteriaCompletada()) {

                imageView.setOpacity(1.0);

                if (nodo.getTipoOriginal() == TipoNodo.NUCLEO) {
                    imageView.setFitWidth(nodo.getRadio() * 3);
                    imageView.setFitHeight(nodo.getRadio() * 3);
                } else {
                    imageView.setFitWidth(nodo.getRadio() * 3);
                    imageView.setFitHeight(nodo.getRadio() * 3);
                }

            } else {
                // El nodo está VIVO o en proceso de INFECCIÓN/TRANSFORMACIÓN
                double nivelInfeccion = nodo.getNivelInfeccion();

                if (nivelInfeccion > 0) {
                    // Parpadeo: usa el seno para crear un efecto de pulsación entre 0.4 y 0.8
                    imageView.setOpacity(0.4 + Math.abs(Math.sin(System.currentTimeMillis() / 250.0)) * 0.4);
                } else {
                    imageView.setOpacity(1.0); // Completamente opaco cuando está sano
                }

                imageView.setFitWidth(nodo.getRadio() * 3);
                imageView.setFitHeight(nodo.getRadio() * 3);
            }
        }

        Iterator<ParticulaRecurso> iterator = particulasActivas.iterator();
        while (iterator.hasNext()) {
            ParticulaRecurso particula = iterator.next();
            if (particula.mover()) {
                // La partícula ha llegado a su destino
                this.getChildren().remove(particula); // Remover visualmente
                iterator.remove(); // Remover de la lista activa
            }
        }
    }
}