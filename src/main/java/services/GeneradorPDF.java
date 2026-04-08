package services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// Genera las facturas en formato PDF usando la librería iText.
// Los datos del negocio se leen de ConfigManager para que el cliente pueda personalizarlos.
// El PDF se guarda en Documentos/Sistema Facturacion/Facturas/.
public class GeneradorPDF {

    // Genera el PDF completo de una factura y lo guarda en disco.
    // Retorna el archivo creado, o null si hubo un error.
    public File generarFacturaPdf(
            String numFac, String rucCi, String nombreCliente,
            String correoCliente, String telefonoCliente, String direccionCliente,
            ArrayList<String[]> productosFactura, // {CANT, #, NOMBRE, PRECIO, DESC, SUB, TOTAL}
            double subtotalVenta, double descuentoVenta, double totalVenta,
            double pagado, double cambio, double deuda,
            String metodoPago, String estado,
            double montoEfectivo, double montoTransferencia, String fechaVencimiento) {

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            // Se guardará en la carpeta del usuario (Mis Documentos/Sistema Facturacion)
            String ruta = System.getProperty("user.home") + File.separator +
                    "Ivencore" + File.separator + "Facturas";
            String nombreArchivo = "Factura_" + numFac + ".pdf";

            File directorio = new File(ruta);
            if (!directorio.exists()) directorio.mkdirs();

            File pdfFile = new File(ruta + File.separator + nombreArchivo);

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            writer.setPageEvent(new HeaderFooter());
            document.open();

            // --- PALETA DE COLORES Y FUENTES ---
            BaseColor COLOR_TITULO  = new BaseColor(50, 50, 50);
            BaseColor COLOR_NEGRO   = new BaseColor(0, 0, 0);
            BaseColor COLOR_GRIS    = new BaseColor(80, 80, 80);
            BaseColor COLOR_GRIS_L  = new BaseColor(200, 200, 200);
            BaseColor COLOR_ROJO    = new BaseColor(200, 0, 0);

            Font fontCompany     = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, COLOR_TITULO);
            Font fontInvoiceTitle = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_ROJO);
            Font fontSubHeader   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_GRIS);
            Font fontTexto       = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, COLOR_NEGRO);
            Font fontTableTitle  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_NEGRO);
            Font fontTotal       = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_TITULO);

            // Leer datos del negocio desde la configuración del usuario
            String negNombre   = utilities.ConfigManager.get("negocio.nombre",    "Tu Negocio");
            String negRuc      = utilities.ConfigManager.get("negocio.ruc",       "0000000000001");
            String negDir      = utilities.ConfigManager.get("negocio.direccion", "Dirección del negocio");
            String negTel      = utilities.ConfigManager.get("negocio.telefono",  "");
            String negEmail    = utilities.ConfigManager.get("negocio.email",     "");

            // --- ENCABEZADO DE LA EMPRESA ---
            PdfPTable topSection = new PdfPTable(2);
            topSection.setWidthPercentage(100);
            topSection.setWidths(new float[]{3f, 2f});
            topSection.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            PdfPTable companyTitle = new PdfPTable(1);
            companyTitle.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            companyTitle.addCell(createCell(negNombre, fontCompany, Element.ALIGN_LEFT));

            PdfPTable datosEmpresa = new PdfPTable(1);
            datosEmpresa.setWidthPercentage(100);
            datosEmpresa.setSpacingBefore(10);
            datosEmpresa.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            datosEmpresa.addCell(createCell(negNombre, fontTotal, Element.ALIGN_LEFT));
            datosEmpresa.addCell(createCell("RUC: " + negRuc, fontTexto, Element.ALIGN_LEFT));
            datosEmpresa.addCell(createCell("Dirección: " + negDir, fontTexto, Element.ALIGN_LEFT));
            datosEmpresa.addCell(createCell("Tel: " + negTel + " | Email: " + negEmail, fontTexto, Element.ALIGN_LEFT));

            PdfPTable invoiceInfo = new PdfPTable(1);
            invoiceInfo.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            invoiceInfo.addCell(createCell("Factura", fontInvoiceTitle, Element.ALIGN_RIGHT));

            topSection.addCell(companyTitle);
            topSection.addCell(invoiceInfo);
            document.add(topSection);
            document.add(datosEmpresa);

            document.add(new Paragraph(" ", new Font(Font.FontFamily.HELVETICA, 1)));
            document.add(createLine(1.0f, COLOR_NEGRO));

            // --- INFO DE FACTURA ---
            PdfPTable infoFactura = new PdfPTable(4);
            infoFactura.setWidthPercentage(100);
            infoFactura.setWidths(new float[]{1f, 1f, 1f, 1f});
            infoFactura.setSpacingBefore(10);
            infoFactura.setSpacingAfter(10);
            infoFactura.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            infoFactura.addCell(createStackedCell("Total", fontSubHeader, "$" + String.format("%.2f", totalVenta), fontTexto, Element.ALIGN_CENTER));
            infoFactura.addCell(createStackedCell("N° Factura", fontSubHeader, numFac, fontTexto, Element.ALIGN_CENTER));
            infoFactura.addCell(createStackedCell("Fecha", fontSubHeader, new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()), fontTexto, Element.ALIGN_CENTER));
            // Vencimiento solo si hay deuda. Si está pagado, mostramos el estado en su lugar.
            if (fechaVencimiento != null && !fechaVencimiento.isEmpty()) {
                infoFactura.addCell(createStackedCell("Vence el", fontSubHeader, fechaVencimiento, fontTexto, Element.ALIGN_CENTER));
            } else {
                infoFactura.addCell(createStackedCell("Estado", fontSubHeader, estado, fontTexto, Element.ALIGN_CENTER));
            }
            document.add(infoFactura);

            document.add(createLine(0.5f, COLOR_NEGRO));

            // --- DATOS DEL CLIENTE ---
            PdfPTable clientTable = new PdfPTable(2);
            clientTable.setWidthPercentage(100);
            clientTable.setWidths(new float[]{1f, 1f});
            clientTable.setSpacingBefore(10);
            clientTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            clientTable.addCell(createCell("Info Cliente", fontTexto, Element.ALIGN_CENTER));
            clientTable.addCell(createCell("Cliente: " + nombreCliente, fontTexto, Element.ALIGN_CENTER));
            clientTable.addCell(createCell("RUC/CI: " + rucCi, fontTexto, Element.ALIGN_CENTER));
            clientTable.addCell(createCell("Dirección: " + direccionCliente, fontTexto, Element.ALIGN_CENTER));
            clientTable.addCell(createCell("Teléfono: " + telefonoCliente, fontTexto, Element.ALIGN_CENTER));
            clientTable.addCell(createCell("Correo: " + correoCliente, fontTexto, Element.ALIGN_CENTER));
            document.add(clientTable);
            document.add(createLine(1.0f, COLOR_NEGRO));

            // --- TABLA DE PRODUCTOS ---
            PdfPTable tablaProductos = new PdfPTable(7);
            tablaProductos.setWidthPercentage(100);
            tablaProductos.setSpacingBefore(15);
            tablaProductos.setWidths(new float[]{0.8f, 1.2f, 3f, 1.5f, 1f, 1.5f, 1.5f});

            for (String header : new String[]{"CANT.", "#", "NOMBRE", "PRECIO UNIT.", "DESC. (%)", "SUBTOTAL", "TOTAL"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, fontTableTitle));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                cell.setBorder(Rectangle.BOTTOM);
                cell.setBorderColor(COLOR_TITULO);
                cell.setBorderWidth(1.0f);
                tablaProductos.addCell(cell);
            }

            for (String[] p : productosFactura) {
                tablaProductos.addCell(createDataCell(p[0], fontTexto, Element.ALIGN_CENTER, COLOR_GRIS_L, 0.5f)); // Cantidad
                tablaProductos.addCell(createDataCell(p[1], fontTexto, Element.ALIGN_CENTER, COLOR_GRIS_L, 0.5f)); // Codigo
                tablaProductos.addCell(createDataCell(p[2], fontTexto, Element.ALIGN_LEFT, COLOR_GRIS_L, 0.5f));   // Nombre
                tablaProductos.addCell(createDataCell("$" + p[3], fontTexto, Element.ALIGN_RIGHT, COLOR_GRIS_L, 0.5f)); // Precio
                tablaProductos.addCell(createDataCell(p[4], fontTexto, Element.ALIGN_RIGHT, COLOR_GRIS_L, 0.5f));  // Desc
                tablaProductos.addCell(createDataCell("$" + p[5], fontTexto, Element.ALIGN_RIGHT, COLOR_GRIS_L, 0.5f)); // Subtotal
                tablaProductos.addCell(createDataCell("$" + p[6], fontTexto, Element.ALIGN_RIGHT, COLOR_GRIS_L, 0.5f)); // Total
            }
            document.add(tablaProductos);

            // --- TOTALES ---
            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(40);
            tablaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaTotales.setSpacingBefore(15);
            tablaTotales.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            tablaTotales.addCell(createCell("SUBTOTAL:", fontSubHeader, Element.ALIGN_LEFT));
            tablaTotales.addCell(createCell("$" + String.format("%.2f", subtotalVenta), fontTexto, Element.ALIGN_RIGHT));
            tablaTotales.addCell(createCell("DESCUENTO:", fontSubHeader, Element.ALIGN_LEFT));
            tablaTotales.addCell(createCell("$" + String.format("%.2f", descuentoVenta), fontTexto, Element.ALIGN_RIGHT));

            PdfPCell tlLabel = createCell("TOTAL:", fontTotal, Element.ALIGN_LEFT);
            tlLabel.setBorder(Rectangle.TOP); tlLabel.setBorderWidth(1f); tlLabel.setBorderColor(COLOR_TITULO);
            PdfPCell tlVal = createCell("$" + String.format("%.2f", totalVenta), fontTotal, Element.ALIGN_RIGHT);
            tlVal.setBorder(Rectangle.TOP); tlVal.setBorderWidth(1f); tlVal.setBorderColor(COLOR_TITULO);
            tablaTotales.addCell(tlLabel);
            tablaTotales.addCell(tlVal);

            // ---> NUEVA LÍNEA: SE MUESTRA EL MÉTODO DE PAGO <---
            tablaTotales.addCell(createCell("MÉTODO PAGO:", fontSubHeader, Element.ALIGN_LEFT));
            tablaTotales.addCell(createCell(metodoPago != null ? metodoPago.toUpperCase() : "N/A", fontTexto, Element.ALIGN_RIGHT));

            tablaTotales.addCell(createCell("PAGADO:", fontSubHeader, Element.ALIGN_LEFT));
            tablaTotales.addCell(createCell("$" + String.format("%.2f", pagado), fontTexto, Element.ALIGN_RIGHT));
            tablaTotales.addCell(createCell("CAMBIO:", fontSubHeader, Element.ALIGN_LEFT));
            tablaTotales.addCell(createCell("$" + String.format("%.2f", cambio), fontTexto, Element.ALIGN_RIGHT));

            PdfPCell deudaLabel = createCell("DEUDA (" + estado.toUpperCase() + "):", fontSubHeader, Element.ALIGN_LEFT);
            deudaLabel.setBorder(Rectangle.TOP); deudaLabel.setBorderWidth(0.5f); deudaLabel.setBorderColor(COLOR_GRIS_L);
            PdfPCell deudaVal = createCell("$" + String.format("%.2f", deuda), fontTexto, Element.ALIGN_RIGHT);
            deudaVal.setBorder(Rectangle.TOP); deudaVal.setBorderWidth(0.5f); deudaVal.setBorderColor(COLOR_GRIS_L);
            tablaTotales.addCell(deudaLabel);
            tablaTotales.addCell(deudaVal);

            document.add(tablaTotales);
            return pdfFile;

        } catch (Exception e) {
            raven.alerts.MessageAlerts.getInstance().showMessage("Error al generar PDF", e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (document.isOpen()) document.close();
        }
    }

    // --- Helpers Privados para construir el PDF ---
    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell createDataCell(String text, Font font, int alignment, BaseColor borderColor, float borderWidth) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(borderWidth);
        cell.setBorderColor(borderColor);
        return cell;
    }

    private PdfPTable createLine(float thickness, BaseColor color) {
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        lineTable.setSpacingBefore(10);
        lineTable.setSpacingAfter(10);
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setFixedHeight(thickness);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(color);
        lineTable.addCell(cell);
        return lineTable;
    }

    private PdfPCell createStackedCell(String title, Font titleFont, String value, Font valueFont, int alignment) {
        PdfPTable nested = new PdfPTable(1);
        nested.setWidthPercentage(100);
        nested.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        PdfPCell titleCell = createCell(title, titleFont, alignment);
        titleCell.setPadding(0);
        nested.addCell(titleCell);
        PdfPCell valueCell = createCell(value, valueFont, alignment);
        valueCell.setPaddingTop(2);
        valueCell.setPaddingBottom(5);
        nested.addCell(valueCell);
        PdfPCell container = new PdfPCell(nested);
        container.setBorder(Rectangle.NO_BORDER);
        container.setPadding(0);
        return container;
    }

    private class HeaderFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Font fontFooter = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(150, 150, 150));
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase("Gracias por su compra.", fontFooter),
                    document.getPageSize().getWidth() / 2, document.bottomMargin() - 15, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), fontFooter),
                    document.getPageSize().getWidth() - document.rightMargin(),
                    document.bottomMargin() - 15, 0);
        }
    }
}