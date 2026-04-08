package services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

// Genera recibos de pago en PDF cuando el cliente abona a su deuda.
// El formato es A5 (más pequeño que una factura).
// Muestra el monto abonado, la deuda anterior y la deuda restante.
public class GeneradorReciboDeuda {

    // Genera el recibo y lo guarda junto a las facturas normales.
    // Retorna el archivo generado, o null si hubo un error.
    public static File generarRecibo(
            String noFacturaOriginal,
            String nombreCliente,
            String cedulaCliente,
            double montoAbonado,
            double deudaAnterior,
            double deudaNueva) {

        String negNombre = utilities.ConfigManager.get("negocio.nombre", "Tu Negocio");
        String negRuc    = utilities.ConfigManager.get("negocio.ruc",    "0000000000001");
        String negDir    = utilities.ConfigManager.get("negocio.direccion", "");
        String negTel    = utilities.ConfigManager.get("negocio.telefono", "");

        String fechaHoy  = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String estado    = deudaNueva <= 0.001 ? "CANCELADO" : "PENDIENTE";

        String ruta = System.getProperty("user.home") + File.separator +
                      "Sistema Facturacion" + File.separator + "Facturas";
        new File(ruta).mkdirs();

        String nombreArchivo = "Recibo_Abono_" + noFacturaOriginal + "_" + timestamp + ".pdf";
        File pdfFile = new File(ruta + File.separator + nombreArchivo);

        Document doc = new Document(PageSize.A5, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(pdfFile));
            doc.open();

            BaseColor TEAL   = new BaseColor(0, 153, 145);
            BaseColor NEGRO  = new BaseColor(30, 41, 59);
            BaseColor GRIS   = new BaseColor(100, 116, 139);
            BaseColor ROJO   = new BaseColor(220, 38, 38);
            BaseColor VERDE  = new BaseColor(5, 150, 105);

            Font fTitulo  = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, TEAL);
            Font fSub     = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD, GRIS);
            Font fNormal  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, NEGRO);
            Font fBold    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   NEGRO);
            Font fMonto   = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   TEAL);
            Font fEstado  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,
                            deudaNueva <= 0.001 ? VERDE : ROJO);

            // ── Encabezado ────────────────────────────────────────────────────
            Paragraph pNeg = new Paragraph(negNombre, fTitulo);
            pNeg.setAlignment(Element.ALIGN_CENTER);
            doc.add(pNeg);

            Paragraph pInfo = new Paragraph(
                "RUC: " + negRuc + "\n" + negDir + (negTel.isEmpty() ? "" : " · Tel: " + negTel),
                new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, GRIS));
            pInfo.setAlignment(Element.ALIGN_CENTER);
            doc.add(pInfo);

            // Línea divisoria
            doc.add(Chunk.NEWLINE);
            PdfPTable lineaTop = new PdfPTable(1);
            lineaTop.setWidthPercentage(100);
            PdfPCell lineaCellTop = new PdfPCell();
            lineaCellTop.setBorderWidthTop(0); lineaCellTop.setBorderWidthLeft(0);
            lineaCellTop.setBorderWidthRight(0); lineaCellTop.setBorderWidthBottom(1.5f);
            lineaCellTop.setBorderColorBottom(TEAL);
            lineaCellTop.setPaddingBottom(6); lineaCellTop.setBorder(Rectangle.BOTTOM);
            lineaTop.addCell(lineaCellTop);
            doc.add(lineaTop);

            // ── Título recibo ────────────────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            Paragraph pTitulo = new Paragraph("RECIBO DE ABONO",
                new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, NEGRO));
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            doc.add(pTitulo);

            Paragraph pRef = new Paragraph("Ref. Factura #" + noFacturaOriginal + "  ·  " + fechaHoy,
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, GRIS));
            pRef.setAlignment(Element.ALIGN_CENTER);
            doc.add(pRef);
            doc.add(Chunk.NEWLINE);

            // ── Datos del cliente ────────────────────────────────────────────
            PdfPTable tCliente = new PdfPTable(2);
            tCliente.setWidthPercentage(100);
            tCliente.setWidths(new float[]{1f, 2f});
            tCliente.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            tCliente.getDefaultCell().setPadding(3);

            addFila(tCliente, "Cliente:", nombreCliente, fSub, fNormal);
            addFila(tCliente, "Cédula/RUC:", cedulaCliente, fSub, fNormal);
            doc.add(tCliente);
            doc.add(Chunk.NEWLINE);

            // ── Monto abonado (grande y destacado) ───────────────────────────
            PdfPTable tMonto = new PdfPTable(1);
            tMonto.setWidthPercentage(100);
            PdfPCell celdaMonto = new PdfPCell();
            celdaMonto.setBorder(Rectangle.BOX);
            celdaMonto.setBorderColor(TEAL);
            celdaMonto.setBorderWidth(1.5f);
            celdaMonto.setBackgroundColor(new BaseColor(224, 247, 244));
            celdaMonto.setPadding(12);
            celdaMonto.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pLabel = new Paragraph("MONTO ABONADO", fSub);
            pLabel.setAlignment(Element.ALIGN_CENTER);
            Paragraph pMonto = new Paragraph("$" + String.format("%.2f", montoAbonado), fMonto);
            pMonto.setAlignment(Element.ALIGN_CENTER);

            celdaMonto.addElement(pLabel);
            celdaMonto.addElement(pMonto);
            tMonto.addCell(celdaMonto);
            doc.add(tMonto);
            doc.add(Chunk.NEWLINE);

            // ── Resumen de deuda ─────────────────────────────────────────────
            PdfPTable tDeuda = new PdfPTable(2);
            tDeuda.setWidthPercentage(100);
            tDeuda.setWidths(new float[]{1.5f, 1f});
            tDeuda.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            tDeuda.getDefaultCell().setPadding(4);

            addFila(tDeuda, "Deuda antes del pago:", "$" + String.format("%.2f", deudaAnterior), fSub, fBold);
            addFila(tDeuda, "Abono registrado:",     "$" + String.format("%.2f", montoAbonado),  fSub, fBold);
            addFila(tDeuda, "Deuda restante:",        "$" + String.format("%.2f", deudaNueva),   fSub,
                new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, deudaNueva <= 0.001 ? VERDE : ROJO));

            doc.add(tDeuda);

            // Línea separadora
            PdfPTable lineaBot = new PdfPTable(1);
            lineaBot.setWidthPercentage(100);
            PdfPCell lc = new PdfPCell();
            lc.setBorder(Rectangle.TOP); lc.setBorderColorTop(new BaseColor(226, 232, 240));
            lc.setPaddingTop(8); lc.setBorderWidthTop(1f);
            lineaBot.addCell(lc);
            doc.add(lineaBot);

            // ── Estado de la cuenta ───────────────────────────────────────────
            Paragraph pEstado = new Paragraph("CUENTA: " + estado, fEstado);
            pEstado.setAlignment(Element.ALIGN_CENTER);
            doc.add(pEstado);

            // ── Pie ───────────────────────────────────────────────────────────
            doc.add(Chunk.NEWLINE);
            Paragraph pPie = new Paragraph(
                "Este documento es un comprobante de abono. Consérvelo como respaldo.\n" + negNombre,
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, GRIS));
            pPie.setAlignment(Element.ALIGN_CENTER);
            doc.add(pPie);

            doc.close();
            return pdfFile;

        } catch (Exception e) {
            System.err.println("GeneradorReciboDeuda: " + e.getMessage());
            if (doc.isOpen()) doc.close();
            return null;
        }
    }

    private static void addFila(PdfPTable t, String label, String valor, Font fLabel, Font fValor) {
        PdfPCell cL = new PdfPCell(new Phrase(label, fLabel));
        cL.setBorder(Rectangle.NO_BORDER); cL.setPadding(3);
        PdfPCell cV = new PdfPCell(new Phrase(valor, fValor));
        cV.setBorder(Rectangle.NO_BORDER); cV.setPadding(3);
        t.addCell(cL); t.addCell(cV);
    }
}
