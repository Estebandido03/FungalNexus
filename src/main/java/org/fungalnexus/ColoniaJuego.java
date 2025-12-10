package org.fungalnexus;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class ColoniaJuego extends Application {

    private GrafoColonia grafoColonia;
    private PanelJuegoFX panelJuego;
    private ConstruccionManager construccionManager;

    private Label lblNutrientes;
    private Label lblDefensas;
    private Label lblSaludNucleo;

    @Override
    public void start(Stage primaryStage) {

        // Inicialización de gestores y el modelo central
        int centroX = Configuracion.MAPA_WIDTH / 2;
        int centroY = Configuracion.MAPA_HEIGHT / 2;

        int nucleoX = ((centroX / Configuracion.GRID_SIZE) * Configuracion.GRID_SIZE) + Configuracion.GRID_SIZE / 2;
        int nucleoY = ((centroY / Configuracion.GRID_SIZE) * Configuracion.GRID_SIZE) + Configuracion.GRID_SIZE / 2;

        this.grafoColonia = new GrafoColonia(nucleoX, nucleoY);
        this.construccionManager = new ConstruccionManager();

        // Configuración de la Interfaz
        BorderPane root = new BorderPane();

        // Panel Central del Juego
        this.panelJuego = new PanelJuegoFX(grafoColonia, construccionManager);
        root.setCenter(panelJuego);

        // Panel de Control (UI)
        HBox controlPanel = crearControlPanel();
        root.setBottom(controlPanel);

        Scene scene = new Scene(root, Configuracion.MAPA_WIDTH, Configuracion.MAPA_HEIGHT + 50);
        primaryStage.setTitle("Gestión de Micelio");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Iniciar el Game Loop
        iniciarGameLoop();
    }

    private HBox crearControlPanel() {
        HBox hbox = new HBox(20); // Aumentamos el espacio a 20
        hbox.setStyle("-fx-padding: 10; -fx-background-color: #222;"); // Fondo oscuro para el panel de control

        // --- Inicializar Etiquetas ---
        lblNutrientes = new Label("Nutrientes: 0.0 / 0");
        lblDefensas = new Label("Defensas: 0.0");
        lblSaludNucleo = new Label("Núcleo: 100%");

        // Darles un estilo básico (ej. texto blanco)
        String labelStyle = "-fx-text-fill: white; -fx-font-size: 14px;";
        lblNutrientes.setStyle(labelStyle);
        lblDefensas.setStyle(labelStyle);
        lblSaludNucleo.setStyle(labelStyle);

        // --- Botones de Construcción ---
        double costoExtractor = TipoNodo.EXTRACTOR.getCosto();
        Button btnExtractor = new Button("Extractor (" + (int)costoExtractor + ")");

        double costoAlmacenamiento = TipoNodo.ALMACENAMIENTO.getCosto();
        Button btnAlmacenamiento = new Button("Almacén (" + (int)costoAlmacenamiento + ")");

        double costoDefensa = TipoNodo.DEFENSA.getCosto();
        Button btnDefensa = new Button("Defensa (" + (int)costoDefensa + ")");

        btnExtractor.setOnAction(e -> construccionManager.seleccionarConstruccion(TipoNodo.EXTRACTOR));
        btnAlmacenamiento.setOnAction(e -> construccionManager.seleccionarConstruccion(TipoNodo.ALMACENAMIENTO));
        btnDefensa.setOnAction(e -> construccionManager.seleccionarConstruccion(TipoNodo.DEFENSA));

        hbox.getChildren().addAll(
                lblSaludNucleo,
                lblNutrientes,
                lblDefensas,
                new Label("|"), // Separador visual
                btnExtractor,
                btnAlmacenamiento,
                btnDefensa
        );
        return hbox;
    }

    private void actualizarEtiquetasUI() {
        double saludNucleo = grafoColonia.getNucleo().getSalud();
        double saludBaseNucleo = TipoNodo.NUCLEO.getSaludBase(); // Obtener la salud máxima
        double nutrientes = grafoColonia.getNutrientesTotales();
        double capacidad = grafoColonia.getCapacidadNutrienteTotal();
        double defensas = grafoColonia.getDefensasTotales();

        // Calcular el porcentaje de salud
        double porcentajeSalud = (saludNucleo / saludBaseNucleo) * 100.0;
        lblSaludNucleo.setText(String.format("Núcleo: %.0f%%", porcentajeSalud));

        // Usar %.0f para la capacidad (que es double en el modelo)
        if (nutrientes >= capacidad * 0.95 && capacidad > 0) {
            lblNutrientes.setText(String.format("Nutrientes: %.1f / %.0f (¡Lleno!)", nutrientes, capacidad));
            lblNutrientes.setStyle("-fx-text-fill: yellow; -fx-font-size: 14px;");
        } else {
            lblNutrientes.setText(String.format("Nutrientes: %.1f / %.0f", nutrientes, capacidad));
            lblNutrientes.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        }

        lblDefensas.setText(String.format("Defensas: %.1f", defensas));

        // Lógica de Derrota: Si el núcleo muere (salud <= 0)
        if (grafoColonia.isGameOver()) { // Usar la bandera de Game Over
            lblSaludNucleo.setText("Núcleo: BACTERIA");
            lblSaludNucleo.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-font-weight: bold;");
        }
    }

    private void iniciarGameLoop() {
        new AnimationTimer() {
            private long lastUpdate = 0;
            private final long updateInterval = 1_000_000_000; // 1 segundo en nanosegundos
            private int ciclosTranscurridos = 0; // NUEVO: Contador de ciclos
            private final int cicloDeInfeccionInicial = 45; // Iniciar después de 10 segundos

            @Override
            public void handle(long now) {
                if (grafoColonia.isGameOver()) {
                    this.stop(); // Detiene el bucle de juego
                    System.out.println("¡Juego Detenido! (AnimationTimer Stop)");
                    return;
                }

                if (now - lastUpdate >= updateInterval) {

                    // --- 1. Lógica del Modelo ---
                    grafoColonia.actualizarRecursos();

                    if (ciclosTranscurridos >= cicloDeInfeccionInicial && grafoColonia.getNodosInfectados().isEmpty()) {
                        grafoColonia.iniciarPrimeraInfeccion();
                    }

                    grafoColonia.actualizarInfeccionYCombate(
                            Configuracion.FACTOR_PROPAGACION_BACTERIA,
                            Configuracion.DANO_BACTERIA_POR_CICLO,
                            Configuracion.COSTO_DEFENSA_POR_COMBATE
                    );

                    // --- 2. Actualización de la Vista ---
                    panelJuego.actualizarVista();
                    actualizarEtiquetasUI();
                    ciclosTranscurridos++;

                    lastUpdate = now;
                }
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
