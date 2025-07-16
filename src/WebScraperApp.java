import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import org.json.*;

public class WebScraperApp extends JFrame {
    private JTextField urlField;
    private JButton fetchButton;
    private JButton downloadCsvButton;
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

        topPanel.add(new JLabel("URL:"));
        topPanel.add(urlField);
        topPanel.add(fetchButton);
        topPanel.add(downloadCsvButton);

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
                    String apiUrl = "http://127.0.0.1:5000/api/productos?url=" + URLEncoder.encode(urlInput, "UTF-8");
                    HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                    connection.setRequestMethod("GET");

                    int status = connection.getResponseCode();
                    InputStream stream;
                    if (status >= 200 && status < 300) {
                        stream = connection.getInputStream();
                    } else {
                        stream = connection.getErrorStream();
                        errorMsg = "HTTP " + status + ": " + readStream(stream);
                        return null;
                    }

                    String response = readStream(stream);
                    JSONArray productos = new JSONArray(response);
                    tableModel.setRowCount(0); // Limpiar tabla
                    for (int i = 0; i < productos.length(); i++) {
                        JSONObject p = productos.getJSONObject(i);
                        tableModel.addRow(new Object[] {
                                p.optString("referencia"),
                                p.optString("nombre"),
                                p.optString("precio"),
                                p.optString("descripcion"),
                                p.optString("foto")
                        });
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

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            response.append(line);
        reader.close();
        return response.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WebScraperApp().setVisible(true);
        });
    }
}
