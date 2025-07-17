import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebScraperApp extends JFrame {
    private JTextField urlField;
    private JButton fetchButton;
    private JButton downloadCsvButton;
    private JButton downloadImagesButton;
    private JButton downloadImagesDefaultButton;
    private JTable table;
    private DefaultTableModel tableModel;

    public WebScraperApp() {
        setTitle("Scraper de Productos");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel superior con input y botón
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());

        urlField = new JTextField("https://www.artesaniabredasegra.com/es/ceramica/cazuelas-de-barro/", 50);
        fetchButton = new JButton("Ver Productos");
        downloadCsvButton = new JButton("Descargar CSV");
        downloadImagesButton = new JButton("Descargar Imágenes");
        downloadImagesDefaultButton = new JButton("Descargar Imágenes (por defecto)");

        topPanel.add(new JLabel("URL:"));
        topPanel.add(urlField);
        topPanel.add(fetchButton);
        topPanel.add(downloadCsvButton);
        topPanel.add(downloadImagesButton);
        topPanel.add(downloadImagesDefaultButton);

        add(topPanel, BorderLayout.NORTH);

        // Tabla
        String[] columnNames = {"Referencia", "Nombre", "Precio", "Descripción", "Imagen URL"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Acción del botón de productos
        fetchButton.addActionListener(e -> {
            String url = urlField.getText().trim();
            fetchProductsAsync(url);
        });

        // Acción del botón de descargar CSV
        downloadCsvButton.addActionListener(e -> {
            descargarCSV();
        });

        // Acción del botón de descargar imágenes
        downloadImagesButton.addActionListener(e -> {
            descargarImagenes();
        });

        // Acción del botón de descarga por defecto
        downloadImagesDefaultButton.addActionListener(e -> {
            descargarImagenesPorDefecto();
        });
    }
    // Descarga todas las imágenes en C:\temp\descarregues_{fecha_hora}
    private void descargarImagenesPorDefecto() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para descargar imágenes.");
            return;
        }
        String baseDir = "C:\\temp";
        File tempDir = new File(baseDir);
        if (!tempDir.exists()) tempDir.mkdirs();
        String fechaHora = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File descargaDir = new File(tempDir, "descarregues_" + fechaHora);
        if (!descargaDir.exists()) descargaDir.mkdirs();
        int count = 0;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object urlObj = tableModel.getValueAt(row, 4); // Columna Imagen URL
            String url = urlObj != null ? urlObj.toString() : "";
            if (url.isEmpty() || !url.startsWith("http")) continue;
            try (InputStream in = new URL(url).openStream()) {
                File imgFile = new File(descargaDir, "img_" + row + ".jpg");
                try (OutputStream out = new FileOutputStream(imgFile)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                count++;
            } catch (Exception ex) {
                // Puedes mostrar un mensaje o ignorar errores individuales
            }
        }
        JOptionPane.showMessageDialog(this, "Descargadas " + count + " imágenes en:\n" + descargaDir.getAbsolutePath());
    }
    // Descarga todas las imágenes de la columna "Imagen URL" en una carpeta elegida
    private void descargarImagenes() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para descargar imágenes.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecciona carpeta destino");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File carpeta = chooser.getSelectedFile();
        int count = 0;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object urlObj = tableModel.getValueAt(row, 4); // Columna Imagen URL
            String url = urlObj != null ? urlObj.toString() : "";
            if (url.isEmpty() || !url.startsWith("http")) continue;
            try (InputStream in = new URL(url).openStream()) {
                File imgFile = new File(carpeta, "img_" + row + ".jpg");
                try (OutputStream out = new FileOutputStream(imgFile)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                count++;
            } catch (Exception ex) {
                // Puedes mostrar un mensaje o ignorar errores individuales
            }
        }
        JOptionPane.showMessageDialog(this, "Descargadas " + count + " imágenes en:\n" + carpeta.getAbsolutePath());
    }

    private void descargarCSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar.");
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como CSV");
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileToSave), "UTF-8"))) {
                // Escribir cabecera
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    pw.print(escapeCsv(tableModel.getColumnName(i)));
                    if (i < tableModel.getColumnCount() - 1) pw.print(",");
                }
                pw.println();
                // Escribir filas
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object value = tableModel.getValueAt(row, col);
                        pw.print(escapeCsv(value != null ? value.toString() : ""));
                        if (col < tableModel.getColumnCount() - 1) pw.print(",");
                    }
                    pw.println();
                }
                JOptionPane.showMessageDialog(this, "CSV guardado correctamente en:\n" + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar CSV:\n" + ex.getMessage());
            }
        }
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return '"' + value + '"';
        }
        return value;
    }

    private void fetchProductsAsync(String urlInput) {
        fetchButton.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private String errorMsg = null;
            @Override
            protected Void doInBackground() {
                try {
                    tableModel.setRowCount(0); // Limpiar tabla
                    // Solo implemento scraping para artesaniabredasegra.com
                    if (urlInput.contains("artesaniabredasegra.com")) {
                        for (int pagina = 1; pagina <= 50; pagina++) {
                            String url = (pagina == 1) ? urlInput : urlInput.replaceAll("/$","") + "/" + pagina;
                            Document doc = Jsoup.connect(url)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                                .timeout(10000)
                                .get();
                            Elements productos = doc.select("li.grid-4.m-grid-6.s-grid-12.compra.padded-inner");
                            if (productos.isEmpty()) break;
                            for (Element producto : productos) {
                                String referencia = "";
                                Element refTag = producto.selectFirst(".ref");
                                if (refTag != null) {
                                    referencia = refTag.text().trim();
                                } else {
                                    Elements posibles = producto.select("span,div,p");
                                    for (Element pos : posibles) {
                                        if (pos.text().toLowerCase().contains("ref")) {
                                            referencia = pos.text().trim();
                                            break;
                                        }
                                    }
                                }
                                Element nombreTag = producto.selectFirst("h2.item-title");
                                String nombre = nombreTag != null ? nombreTag.text().trim() : "";
                                if (!referencia.isEmpty() && nombre.startsWith(referencia)) {
                    nombre = nombre.substring(referencia.length()).replaceFirst("^[\\s:.-]+", "");
                                }
                                Element fotoTag = producto.selectFirst("div.img.item-img-top img");
                                String foto = fotoTag != null ? fotoTag.attr("src") : "";
                                Element precioTag = producto.selectFirst("div.price.clearfix");
                                String precio = precioTag != null ? precioTag.text().trim() : "";
                                Element descripcionTag = producto.selectFirst("p");
                                String descripcion = descripcionTag != null ? descripcionTag.text().trim() : "";
                                tableModel.addRow(new Object[] {
                                    referencia,
                                    nombre,
                                    precio,
                                    descripcion,
                                    foto
                                });
                            }
                        }
                    } else {
                        errorMsg = "Solo se soporta artesaniabredasegra.com en esta demo.";
                    }
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                fetchButton.setEnabled(true);
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(WebScraperApp.this, "Error al obtener productos:\n" + errorMsg);
                }
            }
        };
        worker.execute();
    }

    // Eliminado: readStream, ya no es necesario

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WebScraperApp().setVisible(true);
        });
    }
}
