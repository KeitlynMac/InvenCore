package services;

import dao.*;
import Model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

// Genera reportes en Excel (.xlsx) usando Apache POI.
// Diseño minimalista: sin líneas de cuadrícula, tipografía limpia, filas alternas.
// Cada tipo de reporte tiene su color de acento propio.
public class ReporteService {

    private static final String CARPETA = System.getProperty("user.home")
            + File.separator + "Sistema Facturacion" + File.separator + "Reportes";

    // Paleta neutral — solo el color de acento cambia por reporte
    private static final byte[] BLANCO     = h("FFFFFF");
    private static final byte[] NEGRO_S    = h("1E293B"); // texto principal
    private static final byte[] GRIS_M     = h("64748B"); // texto secundario
    private static final byte[] GRIS_F     = h("F8FAFC"); // fondo fila alterna
    private static final byte[] BORDE      = h("E2E8F0"); // separadores
    private static final byte[] VERDE      = h("059669");
    private static final byte[] VERDE_BG   = h("ECFDF5");
    private static final byte[] ROJO       = h("DC2626");
    private static final byte[] ROJO_BG    = h("FEF2F2");
    private static final byte[] NARANJA    = h("D97706");
    private static final byte[] NARANJA_BG = h("FFFBEB");
    // Colores de acento por reporte
    private static final byte[] AZUL       = h("2563EB");
    private static final byte[] VIOLETA    = h("7C3AED");

    // ─────────────────────────────────────────────────────────────────────────
    // REPORTES PÚBLICOS
    // ─────────────────────────────────────────────────────────────────────────

    public static File reporteVentas(String mes) throws IOException {
        String suf = mes != null && !mes.isEmpty() ? "_" + mes : "_historico";
        File f = archivo("Ventas" + suf);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet h = sheet(wb, "Ventas");
            S s = new S(wb, AZUL);
            int row = head(h, s, "Reporte de Ventas",
                    mes != null && !mes.isEmpty() ? "Período: " + mes : "Período: Histórico",
                    new String[]{"N° Factura","Cédula/RUC","Cliente","Fecha","Total Factura","Pagado","Deuda","Estado","Método"},
                    new int[]{12,14,28,14,16,14,14,13,14});

            List<Object[]> data = new FacturaDAO().listarFacturas(null, mes);
            double sTotal = 0, sDeuda = 0;
            for (int i = 0; i < data.size(); i++) {
                Object[] v = data.get(i);
                boolean alt = i % 2 == 1;
                Row r = row(h, row++, 22);
                double monto = num(str(v[4]));
                double deuda = v.length > 6 ? num(str(v[6])) : 0;
                sTotal += monto; sDeuda += deuda;
                c(r,0,str(v[0]),s.n(alt)); c(r,1,str(v[1]),s.n(alt));
                c(r,2,str(v[2]),s.n(alt)); c(r,3,str(v[3]),s.n(alt));
                cd(r,4,monto,s.dp(alt)); cd(r,5,num(str(v.length>5?v[5]:null)),s.dp(alt));
                cd(r,6,deuda,deuda>0?s.dn(alt):s.dp(alt));
                badge(r,7,wb,str(v.length>5?v[5]:null).equalsIgnoreCase("Pagado")?"Pagado":"Por Pagar",
                    str(v.length>5?v[5]:null).equalsIgnoreCase("Pagado")?VERDE:ROJO,
                    str(v.length>5?v[5]:null).equalsIgnoreCase("Pagado")?VERDE_BG:ROJO_BG);
                c(r,8,str(v.length>8?v[8]:null),s.n(alt));
            }
            totales(h, row, s, new int[]{4,6}, new double[]{sTotal,sDeuda},
                    new String[]{"","","","TOTAL — "+data.size()+" facturas","","","","",""});
            h.setAutoFilter(new CellRangeAddress(5,5,0,8));
            save(wb,f);
        }
        return f;
    }

    // Reporte del estado actual del inventario.
    public static File reporteInventario() throws IOException {
        File f = archivo("Inventario");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet h = sheet(wb, "Inventario");
            S s = new S(wb, VERDE);
            int row = head(h, s, "Inventario de Productos",
                    "Corte: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()),
                    new String[]{"Código","Nombre","Categoría","Precio","Stock","Estado","Valor Stock"},
                    new int[]{12,32,18,14,10,13,16});

            List<Producto> ps = new ProductoDAO().obtenerTodos();
            double total = 0; int agot = 0, poco = 0;
            for (int i = 0; i < ps.size(); i++) {
                Producto p = ps.get(i);
                boolean alt = i%2==1;
                Row r = row(h,row++,22);
                double val = p.getPrecio()*p.getStock(); total+=val;
                c(r,0,p.getCodigo(),s.n(alt)); c(r,1,p.getNombre(),s.n(alt));
                c(r,2,nv(p.getCategoria()),s.n(alt));
                cd(r,3,p.getPrecio(),s.dp(alt)); cn(r,4,p.getStock(),s.nc(alt));
                byte[] fg,bg; String est;
                if (p.getStock()==0){est="Agotado";fg=ROJO;bg=ROJO_BG;agot++;}
                else if(p.getStock()<=3){est="Poco";fg=NARANJA;bg=NARANJA_BG;poco++;}
                else{est="OK";fg=VERDE;bg=VERDE_BG;}
                badge(r,5,wb,est,fg,bg);
                cd(r,6,val,s.dp(alt));
            }
            totales(h,row,s,new int[]{6},new double[]{total},
                    new String[]{"","","","","","Valor total:",""});
            // Resumen
            row+=3; Row rr=row(h,row++,20); ctit(rr,0,"▸  RESUMEN",wb,s.acento);
            for(String li:new String[]{ps.size()+" productos",agot+" agotados",poco+" con poco stock"}){
                Row ri=row(h,row++,18); c(ri,1,li,s.n(false));
            }
            h.setAutoFilter(new CellRangeAddress(5,5,0,6));
            save(wb,f);
        }
        return f;
    }

    // Reporte de gastos del período indicado.
    public static File reporteGastos(String mes) throws IOException {
        String suf = mes!=null&&!mes.isEmpty()?"_"+mes:"_historico";
        File f = archivo("Gastos"+suf);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet h = sheet(wb,"Gastos");
            S s = new S(wb,ROJO);
            int row = head(h,s,"Gastos y Egresos",
                    mes!=null&&!mes.isEmpty()?"Período: "+mes:"Período: Histórico",
                    new String[]{"Fecha","Descripción","Categoría","Monto","Notas"},
                    new int[]{14,36,18,14,30});

            List<Gasto> gs = new GastosDAO().buscar(null,mes);
            double total=0; Map<String,Double> porCat=new LinkedHashMap<>();
            for(int i=0;i<gs.size();i++){
                Gasto g=gs.get(i); boolean alt=i%2==1;
                Row r=row(h,row++,22);
                c(r,0,nv(g.getFecha()),s.n(alt)); c(r,1,nv(g.getDescripcion()),s.n(alt));
                c(r,2,nv(g.getCategoria()),s.n(alt)); cd(r,3,g.getMonto(),s.dn(alt));
                c(r,4,nv(g.getNotas()),s.ng(alt));
                total+=g.getMonto();
                porCat.merge(nv2(g.getCategoria(),"Sin categoría"),g.getMonto(),Double::sum);
            }
            totales(h,row,s,new int[]{3},new double[]{total},
                    new String[]{"","","TOTAL:","",""});
            // Por categoría
            row+=3; Row rt=row(h,row++,20); ctit(rt,0,"▸  POR CATEGORÍA",wb,ROJO);
            for(Map.Entry<String,Double> e:porCat.entrySet()){
                Row r=row(h,row++,20);
                c(r,1,e.getKey(),s.n(false)); cd(r,2,e.getValue(),s.dn(false));
                // Barra proporcional simple
                int pct=total>0?(int)(e.getValue()/total*15):0;
                c(r,3,"▬".repeat(Math.max(1,pct)),s.ng(false));
            }
            h.setAutoFilter(new CellRangeAddress(5,5,0,4));
            save(wb,f);
        }
        return f;
    }

    // Balance de ingresos vs gastos de los últimos 12 meses.
    public static File reporteBalance() throws IOException {
        File f = archivo("Balance");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet h = sheet(wb,"Balance");
            S s = new S(wb,VIOLETA);
            int row = head(h,s,"Balance General",
                    "Ingresos vs Gastos — Últimos 12 meses",
                    new String[]{"Mes","Ingresos","Gastos","Balance Neto","Margen %"},
                    new int[]{16,16,16,16,14});

            DashboardDAO dao=new DashboardDAO(); GastosDAO gd=new GastosDAO();
            double tI=0,tG=0;
            for(int i=0;i<12;i++){
                String mes=mesHace(11-i); boolean alt=i%2==1;
                double ing=dao.ingresoMes(mes),gas=gd.totalMes(mes),bal=ing-gas;
                double marg=ing>0?bal/ing*100:0;
                tI+=ing; tG+=gas;
                Row r=row(h,row++,22);
                c(r,0,mes,s.n(alt)); cd(r,1,ing,s.dp(alt)); cd(r,2,gas,s.dn(alt));
                cd(r,3,bal,bal>=0?s.dp(alt):s.dn(alt));
                pct(r,4,wb,marg,alt,marg>=0);
            }
            totales(h,row,s,new int[]{1,2,3},new double[]{tI,tG,tI-tG},
                    new String[]{"TOTALES","","","",""});
            save(wb,f);
        }
        return f;
    }

    // Ranking de los productos más vendidos por unidades.
    public static File reporteProductosVendidos() throws IOException {
        File f = archivo("Top_Productos");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet h = sheet(wb,"Top Productos");
            S s = new S(wb,NARANJA);
            int row = head(h,s,"Productos Más Vendidos",
                    "Ranking histórico por unidades vendidas",
                    new String[]{"#","Producto","Unidades","Ingreso Total","Precio Actual"},
                    new int[]{6,34,16,18,16});

            List<Object[]> data=prodVendidos(); double tIng=0; long tUnd=0;
            for(int i=0;i<data.size();i++){
                Object[] v=data.get(i); boolean alt=i%2==1;
                long unds=((Number)v[1]).longValue(); double ing=((Number)v[2]).doubleValue();
                tIng+=ing; tUnd+=unds;
                Row r=row(h,row++,22);
                c(r,0,i==0?"🥇":i==1?"🥈":i==2?"🥉":String.valueOf(i+1),s.nc(alt));
                c(r,1,str(v[0]),s.n(alt)); cn(r,2,unds,s.nc(alt));
                cd(r,3,ing,s.dp(alt)); cd(r,4,((Number)v[3]).doubleValue(),s.dp(alt));
            }
            totales(h,row,s,new int[]{3},new double[]{tIng},
                    new String[]{"","TOTALES — "+data.size()+" productos",String.valueOf(tUnd)+" uds","",""});
            save(wb,f);
        }
        return f;
    }

    // Lista de facturas pendientes de pago con detalle de deuda.
    public static File reporteCuentasCobrar() throws IOException {
        File f = archivo("CuentasCobrar");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet h = sheet(wb,"Cuentas por Cobrar");
            S s = new S(wb,ROJO);
            int row = head(h,s,"Cuentas por Cobrar",
                    "Facturas pendientes al "+new SimpleDateFormat("dd/MM/yyyy").format(new Date()),
                    new String[]{"N° Factura","Cliente","Cédula/RUC","Fecha Venta","Monto","Deuda","Vencimiento","Estado"},
                    new int[]{14,28,14,14,14,14,14,13});

            List<Object[]> cxc=new FacturaDAO().listarCuentasPorCobrar();
            double sDeuda=0; int venc=0;
            String hoy=new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            for(int i=0;i<cxc.size();i++){
                Object[] v=cxc.get(i); boolean alt=i%2==1;
                double deuda=v.length>6?num(str(v[6])):0;
                String fv=v.length>7&&v[7]!=null?v[7].toString():"";
                boolean vencida=!fv.isEmpty()&&fv.compareTo(hoy)<0;
                sDeuda+=deuda; if(vencida)venc++;
                Row r=row(h,row++,22);
                c(r,0,str(v[0]),s.n(alt)); c(r,1,str(v[1]),s.n(alt));
                c(r,2,str(v[2]),s.n(alt)); c(r,3,str(v[3]),s.n(alt));
                cd(r,4,v.length>4?num(str(v[4])):0,s.dn(alt));
                cd(r,5,deuda,s.dn(alt));
                c(r,6,fv,s.n(alt));
                badge(r,7,wb,vencida?"VENCIDA":"Pendiente",
                    vencida?ROJO:NARANJA,vencida?ROJO_BG:NARANJA_BG);
            }
            totales(h,row,s,new int[]{5},new double[]{sDeuda},
                    new String[]{"","","","TOTAL PENDIENTE:","","",venc+" vencidas",""});
            h.setAutoFilter(new CellRangeAddress(5,5,0,7));
            save(wb,f);
        }
        return f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CLASE INTERNA DE ESTILOS — caché para no crear duplicados
    // ─────────────────────────────────────────────────────────────────────────

    private static class S {
        final XSSFWorkbook wb;
        final byte[] acento;
        private final Map<String,XSSFCellStyle> cache = new HashMap<>();

        S(XSSFWorkbook wb, byte[] acento){ this.wb=wb; this.acento=acento; }

        XSSFCellStyle n(boolean alt)  { return g("n"+alt,  ()->base(alt,false,false)); }
        XSSFCellStyle ng(boolean alt) { return g("ng"+alt, ()->base(alt,false,true)); }
        XSSFCellStyle nc(boolean alt) { return g("nc"+alt, ()->{ var s=base(alt,false,false); s.setAlignment(HorizontalAlignment.CENTER); return s; }); }
        XSSFCellStyle dp(boolean alt) { return g("dp"+alt, ()->dinero(alt,VERDE)); }
        XSSFCellStyle dn(boolean alt) { return g("dn"+alt, ()->dinero(alt,ROJO)); }
        XSSFCellStyle tit()  { return g("tit",  this::makeTit); }
        XSSFCellStyle sub()  { return g("sub",  this::makeSub); }
        XSSFCellStyle hdr()  { return g("hdr",  this::makeHdr); }
        XSSFCellStyle tot()  { return g("tot",  this::makeTot); }

        private XSSFCellStyle g(String k, Supplier<XSSFCellStyle> mk){ return cache.computeIfAbsent(k,x->mk.get()); }

        private XSSFFont fnt(int sz, boolean bold, byte[] col){
            XSSFFont f=wb.createFont(); f.setFontName("Calibri");
            f.setFontHeightInPoints((short)sz); f.setBold(bold);
            if(col!=null) f.setColor(new XSSFColor(col,new DefaultIndexedColorMap()));
            return f;
        }
        private void bg(XSSFCellStyle s,byte[] c){ s.setFillForegroundColor(new XSSFColor(c,new DefaultIndexedColorMap())); s.setFillPattern(FillPatternType.SOLID_FOREGROUND); }
        private void bot(XSSFCellStyle s){ s.setBorderBottom(BorderStyle.THIN); s.setBottomBorderColor(new XSSFColor(BORDE,new DefaultIndexedColorMap())); }

        private XSSFCellStyle base(boolean alt, boolean bold, boolean gris){
            XSSFCellStyle s=wb.createCellStyle(); s.setFont(fnt(11,bold,gris?GRIS_M:NEGRO_S));
            if(alt) bg(s,GRIS_F); bot(s); s.setVerticalAlignment(VerticalAlignment.CENTER); return s;
        }
        private XSSFCellStyle dinero(boolean alt, byte[] col){
            XSSFCellStyle s=wb.createCellStyle(); s.setFont(fnt(11,true,col));
            if(alt) bg(s,GRIS_F); bot(s);
            s.setDataFormat(wb.createDataFormat().getFormat("\"$\"#,##0.00"));
            s.setAlignment(HorizontalAlignment.RIGHT); s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }
        private XSSFCellStyle makeTit(){
            XSSFCellStyle s=wb.createCellStyle(); s.setFont(fnt(22,true,acento));
            bg(s,BLANCO); s.setBorderBottom(BorderStyle.MEDIUM);
            s.setBottomBorderColor(new XSSFColor(acento,new DefaultIndexedColorMap()));
            s.setVerticalAlignment(VerticalAlignment.BOTTOM); return s;
        }
        private XSSFCellStyle makeSub(){
            XSSFCellStyle s=wb.createCellStyle(); s.setFont(fnt(10,false,GRIS_M));
            s.setVerticalAlignment(VerticalAlignment.TOP); return s;
        }
        private XSSFCellStyle makeHdr(){
            XSSFCellStyle s=wb.createCellStyle(); s.setFont(fnt(11,true,NEGRO_S));
            bg(s,GRIS_F); s.setBorderBottom(BorderStyle.MEDIUM);
            s.setBottomBorderColor(new XSSFColor(acento,new DefaultIndexedColorMap()));
            s.setAlignment(HorizontalAlignment.CENTER); s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }
        private XSSFCellStyle makeTot(){
            XSSFCellStyle s=wb.createCellStyle(); s.setFont(fnt(12,true,acento));
            bg(s,BLANCO); s.setBorderTop(BorderStyle.MEDIUM);
            s.setTopBorderColor(new XSSFColor(acento,new DefaultIndexedColorMap()));
            s.setDataFormat(wb.createDataFormat().getFormat("\"$\"#,##0.00"));
            s.setAlignment(HorizontalAlignment.RIGHT); s.setVerticalAlignment(VerticalAlignment.CENTER);
            return s;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private static XSSFSheet sheet(XSSFWorkbook wb, String name){
        XSSFSheet h=wb.createSheet(name);
        h.setDefaultRowHeightInPoints(22);
        h.setDisplayGridlines(false);
        h.getPrintSetup().setLandscape(true);
        h.getPrintSetup().setFitWidth((short)1);
        h.setMargin(Sheet.LeftMargin,0.6); h.setMargin(Sheet.RightMargin,0.6);
        h.setMargin(Sheet.TopMargin,0.8);  h.setMargin(Sheet.BottomMargin,0.8);
        return h;
    }

    private static int head(XSSFSheet h, S s, String tit, String sub, String[] cols, int[] anchos){
        int fila=0;
        // Franja de color
        Row fr=h.createRow(fila++); fr.setHeightInPoints(6);
        for(int i=0;i<cols.length;i++){
            XSSFCellStyle cs=h.getWorkbook().createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(s.acento,new DefaultIndexedColorMap()));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            fr.createCell(i).setCellStyle(cs);
        }
        // Título
        Row rt=h.createRow(fila++); rt.setHeightInPoints(40);
        Cell ct=rt.createCell(0); ct.setCellValue(tit); ct.setCellStyle(s.tit());
        h.addMergedRegion(new CellRangeAddress(fila-1,fila-1,0,cols.length-1));
        // Subtítulo + fecha
        Row rs=h.createRow(fila++); rs.setHeightInPoints(18);
        Cell cs=rs.createCell(0);
        cs.setCellValue(sub+"     |     "+new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
        cs.setCellStyle(s.sub());
        h.addMergedRegion(new CellRangeAddress(fila-1,fila-1,0,cols.length-1));
        // Espacio
        h.createRow(fila++).setHeightInPoints(10);
        // Headers
        Row rh=h.createRow(fila++); rh.setHeightInPoints(28);
        for(int i=0;i<cols.length;i++){
            Cell c=rh.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(s.hdr());
            h.setColumnWidth(i,anchos[i]*256);
        }
        return fila;
    }

    private static void totales(XSSFSheet h, int fila, S s, int[] cdins, double[] vals, String[] txts){
        h.createRow(fila).setHeightInPoints(6);
        Row r=h.createRow(fila+1); r.setHeightInPoints(28);
        for(int c2=0;c2<txts.length;c2++){
            Cell cell=r.createCell(c2); boolean din=false;
            for(int x=0;x<cdins.length;x++) if(cdins[x]==c2){cell.setCellValue(vals[x]);din=true;break;}
            if(!din) cell.setCellValue(txts[c2]);
            cell.setCellStyle(s.tot());
        }
    }

    private static Row row(XSSFSheet h, int n, float ht){ Row r=h.createRow(n); r.setHeightInPoints(ht); return r; }

    private static void c(Row r, int c, String v, CellStyle cs){ Cell x=r.createCell(c); x.setCellValue(v!=null?v:""); if(cs!=null)x.setCellStyle(cs); }
    private static void cd(Row r, int c, double v, CellStyle cs){ Cell x=r.createCell(c); x.setCellValue(v); if(cs!=null)x.setCellStyle(cs); }
    private static void cn(Row r, int c, Number v, CellStyle cs){ Cell x=r.createCell(c); x.setCellValue(v!=null?v.doubleValue():0); if(cs!=null)x.setCellStyle(cs); }

    private static void badge(Row r, int c, XSSFWorkbook wb, String txt, byte[] fg, byte[] bg){
        XSSFCellStyle cs=wb.createCellStyle();
        XSSFFont f=wb.createFont(); f.setFontName("Calibri"); f.setFontHeightInPoints((short)10); f.setBold(true);
        f.setColor(new XSSFColor(fg,new DefaultIndexedColorMap())); cs.setFont(f);
        cs.setFillForegroundColor(new XSSFColor(bg,new DefaultIndexedColorMap()));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.CENTER); cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setBorderBottom(BorderStyle.THIN); cs.setBottomBorderColor(new XSSFColor(BORDE,new DefaultIndexedColorMap()));
        Cell x=r.createCell(c); x.setCellValue(txt); x.setCellStyle(cs);
    }

    private static void pct(Row r, int c, XSSFWorkbook wb, double v, boolean alt, boolean pos){
        XSSFCellStyle cs=wb.createCellStyle(); XSSFFont f=wb.createFont();
        f.setFontName("Calibri"); f.setFontHeightInPoints((short)11); f.setBold(true);
        f.setColor(new XSSFColor(pos?VERDE:ROJO,new DefaultIndexedColorMap())); cs.setFont(f);
        if(alt){cs.setFillForegroundColor(new XSSFColor(GRIS_F,new DefaultIndexedColorMap())); cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);}
        cs.setDataFormat(wb.createDataFormat().getFormat("0.0\"%\""));
        cs.setAlignment(HorizontalAlignment.CENTER); cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setBorderBottom(BorderStyle.THIN); cs.setBottomBorderColor(new XSSFColor(BORDE,new DefaultIndexedColorMap()));
        Cell x=r.createCell(c); x.setCellValue(v); x.setCellStyle(cs);
    }

    private static void ctit(Row r, int c, String v, XSSFWorkbook wb, byte[] col){
        XSSFCellStyle cs=wb.createCellStyle(); XSSFFont f=wb.createFont();
        f.setFontName("Calibri"); f.setFontHeightInPoints((short)12); f.setBold(true);
        f.setColor(new XSSFColor(col,new DefaultIndexedColorMap())); cs.setFont(f);
        Cell x=r.createCell(c); x.setCellValue(v); x.setCellStyle(cs);
    }

    private static File archivo(String nombre) throws IOException {
        File d=new File(CARPETA); d.mkdirs();
        return new File(d,"Reporte_"+nombre+"_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date())+".xlsx");
    }
    private static void save(XSSFWorkbook wb, File f) throws IOException {
        try(FileOutputStream o=new FileOutputStream(f)){wb.write(o);}
    }

    private static List<Object[]> prodVendidos(){
        List<Object[]> l=new ArrayList<>();
        String sql="SELECT p.Nombre,SUM(d.CantidadProductos),SUM(d.CantidadProductos*d.PrecioVenta),p.Precio "+
                   "FROM Detalle_Factura d INNER JOIN Producto p ON d.Producto_IdProducto=p.IdProducto "+
                   "GROUP BY p.IdProducto ORDER BY 2 DESC";
        try(java.sql.Connection c=Conexion.getConnection(); java.sql.PreparedStatement ps=c.prepareStatement(sql); java.sql.ResultSet rs=ps.executeQuery()){
            while(rs.next()) l.add(new Object[]{rs.getString(1),rs.getLong(2),rs.getDouble(3),rs.getDouble(4)});
        }catch(Exception e){System.err.println(e.getMessage());}
        return l;
    }

    private static String mesHace(int mesesAtras){
        java.util.Calendar cal=java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH,-mesesAtras);
        return new SimpleDateFormat("yyyy-MM").format(cal.getTime());
    }

    private static byte[] h(String hex){ hex=hex.replace("#",""); return new byte[]{(byte)Integer.parseInt(hex.substring(0,2),16),(byte)Integer.parseInt(hex.substring(2,4),16),(byte)Integer.parseInt(hex.substring(4,6),16)}; }
    private static String str(Object o){ return o!=null?o.toString():""; }
    private static String nv(String s){ return s!=null?s:""; }
    private static String nv2(String s,String def){ return s!=null&&!s.isEmpty()?s:def; }
    private static double num(String s){ if(s==null||s.isBlank())return 0; try{return Double.parseDouble(s.replace("$","").replace(",","").trim());}catch(Exception e){return 0;} }
}
