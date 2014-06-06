 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessException;
import bo.com.kibo.bl.exceptions.BusinessExceptionMessage;
import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IGenericoBO;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.mailbot.intf.IInterpretadorFormularioDasometrico;
import bo.com.kibo.mailbot.intf.IInterpretadorMensaje;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import org.apache.poi.hssf.util.CellRangeAddressList;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 *
 * @author Olvinho
 * @param <T> Clase entidad
 * @param <ID> Clase que representa el id
 * @param <BO> Clase de negocio
 */
public abstract class InterpretadorMensajeGenerico<T, ID extends Serializable, BO extends IGenericoBO<T, ID>> implements IInterpretadorMensaje {

    private static final ThreadLocal<Map<String, IInterpretadorMensaje>> caja = new ThreadLocal<>();

    public static Map<String, IInterpretadorMensaje> getMapaObjetos() {
        Map<String, IInterpretadorMensaje> mapa = caja.get();
        if (mapa == null) {
            mapa = new HashMap<>();
            mapa.put("area", new InterpretadorMensajeArea());
            mapa.put("faja", new InterpretadorMensajeFaja());
            mapa.put("especie", new InterpretadorMensajeEspecie());
            mapa.put("calidad", new IntepretadorMensajeCalidad());
            mapa.put("carga", new InterpretadorMensajeCarga());
            mapa.put("censo", new InterpretadorMensajeCenso());
            mapa.put("dasometrico", new InterpretarPlantillaFormulario());
            caja.set(mapa);
        }
        return mapa;
    }

    protected Integer idUsuario;
    protected String parametros;
    protected String nombreEntidad;
    protected List<File> archivosTemporales = new ArrayList<>();
    protected Sheet hojaActual;
    private BusinessException mensajesError;
    private boolean cargarPlantillaFormularios;

    public InterpretadorMensajeGenerico() {
        parametros = "";
        cargarPlantillaFormularios = true;
    }

    @Override
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    @Override
    public void setParametros(String parametros) {
        this.parametros = parametros;
    }

    public boolean isCargarPlantillaFormularios() {
        return cargarPlantillaFormularios;
    }

    public void setCargarPlantillaFormularios(boolean cargarPlantillaFormularios) {
        this.cargarPlantillaFormularios = cargarPlantillaFormularios;
    }
    

    @Override
    public Multipart interpretar() throws MessagingException, IOException {
        if (parametros == null || "".equals(parametros)) {
            return null;
        }
        String[] params = parametros.split(UtilitariosMensajes.SEPERADOR_PARAMETROS);
        if (params.length == 0) {
            params = new String[]{parametros};
        }
        String idCargar = "";
        if (params.length > 1) {
            idCargar = params[1];
        }
        switch (params[0]) {
            case "plantilla":
                return enviarPlantilla(true, idCargar);
            case "cargar":
                return enviarPlantilla(false, idCargar);
        }
        return null;
    }

    protected void appendException(BusinessExceptionMessage message) {
        if (mensajesError == null) {
            mensajesError = new BusinessException(message);
            return;
        }
        mensajesError.getMessages().add(message);
    }

    protected Multipart enviarPlantilla(boolean plantillaNueva, String idCargar) throws MessagingException, IOException {
        String nombreArchivoOrigen;
        List<T> lista = null;
        T entidad = null;
        if (!plantillaNueva) {
            if ("todos".equals(idCargar)) {
                lista = getObjetoNegocio().obtenerTodos();
                nombreArchivoOrigen = nombreEntidad + "-" + "lista";
            } else {
                ID id;
                try {
                    id = convertirId(idCargar);
                } catch (Exception ex) {
                    return enviarIdCargarNoValido();
                }
                entidad = getObjetoNegocio().recuperarPorId(id);
                if (entidad == null) {
                    return enviarEntidadNoExiste(idCargar);
                }
                nombreArchivoOrigen = nombreEntidad;
            }
        } else {
            nombreArchivoOrigen = nombreEntidad; 
            if (this instanceof IInterpretadorFormularioDasometrico){
                if (cargarPlantillaFormularios){
                      nombreArchivoOrigen = "plantillafrm";
                }
            }
        }

        String nombreArchivoOriginal = "plantillas/" + nombreArchivoOrigen + ".xlsx";
        File archivoCopia = UtilitariosMensajes.reservarNombre(nombreEntidad);
        UtilitariosMensajes.copiarArchivo(new File(nombreArchivoOriginal), archivoCopia);
        archivosTemporales.add(archivoCopia);
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            Workbook libro;
            fis = new FileInputStream(archivoCopia);
            libro = WorkbookFactory.create(fis);
            hojaActual = libro.getSheetAt(0);
            if (plantillaNueva) {
                preparPlantillaAntesDeEnviar();
            } else {
                if (lista != null) {
                    mostrarLista(lista);
                } else {
                    mostrarEntidad(entidad);
                }
            }
            //Guardamos cambio
            os = new FileOutputStream(archivoCopia);
            libro.write(os);
        } catch (InvalidFormatException ex) {

        } finally {
            if (fis != null) {
                fis.close();
            }
            if (os != null) {
                os.close();
            }
        }
        String textoMensaje;
        if (plantillaNueva) {
            textoMensaje = "La plantilla está adjunta a este mensaje.";
        } else if (lista != null) {
            textoMensaje = "La consulta ha devuelto " + lista.size() + " registro(s).";
        } else {
            textoMensaje = "El registro solicitado está adjunto a este mensaje";
        }
        Multipart cuerpo = new MimeMultipart();
        BodyPart mensaje = new MimeBodyPart();
        mensaje.setText(textoMensaje);
        BodyPart adjunto = new MimeBodyPart();
        DataSource origen = new FileDataSource(archivoCopia);
        adjunto.setDataHandler(new DataHandler(origen));
        adjunto.setFileName(nombreEntidad + ".xlsx");
        cuerpo.addBodyPart(mensaje);
        cuerpo.addBodyPart(adjunto);
        return cuerpo;
    }

    @Override
    public void setNombreEntidad(String nombre) {
        this.nombreEntidad = nombre;
    }

    @Override
    public List<File> obtenerArchivoTemporalesCreados() {
        return archivosTemporales;
    }

    protected Cell getCelda(int rowIndex, int colIndex) {
        Row fila = hojaActual.getRow(rowIndex);
        if (fila == null) {
            fila = hojaActual.createRow(rowIndex);
        }

        Cell celda = fila.getCell(colIndex);
        if (celda == null) {
            celda = fila.createCell(colIndex);
        }

        return celda;

    }

    public static Multipart enviarErroresNegocio(BusinessException errores) throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("No se pudo completar la petición debido a los siguiente errores: \n");
        for (BusinessExceptionMessage error : errores.getMessages()) {
            mensaje.append("-");
            if (error.getIndex() > 0) {
                mensaje.append("Fila ").append(error.getIndex()).append(" --> ");
            }
            mensaje.append(error.getMessage()).append("\n");
        }
        BodyPart texto = new MimeBodyPart();
        texto.setText(mensaje.toString());
        cuerpo.addBodyPart(texto);
        return cuerpo;
    }

    protected Multipart enviarErrorInesperado() throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("Ha ocurrido un error inesperado, por favor intentelo nuevamente");
        cuerpo.addBodyPart(texto);
        return cuerpo;
    }

    protected Multipart enviarEntidadNoExiste(String id) throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("El regitro con Id :" + id + " no existe");
        cuerpo.addBodyPart(texto);
        return cuerpo;
    }

    protected Multipart enviarInserccionExitosa(T entidad) throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("La insercción se ha efectuado exitosamente. El identificador del registro es: " + getId(entidad));
        cuerpo.addBodyPart(texto);
        return cuerpo;

    }

    protected Multipart enviarIdCargarNoValido() throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("No se pudo recuperar el Id especificado, debe enviar un número válido o la cadena todos para recuperar todos los registros");
        cuerpo.addBodyPart(texto);
        return cuerpo;

    }

    protected Multipart enviarModificacionExitosa(T entidad) throws MessagingException {
        Multipart cuerpo = new MimeMultipart();
        BodyPart texto = new MimeBodyPart();
        texto.setText("Se ha efectuado la actualización exitosamente");
        cuerpo.addBodyPart(texto);
        return cuerpo;
    }

    @Override
    public Multipart interpretarHojaExcel(Sheet hojaExcel) throws MessagingException {
        this.hojaActual = hojaExcel;
        mensajesError = null;
        T entidad = convertirHojaEnEntidad();
        if (mensajesError != null) {
            return enviarErroresNegocio(mensajesError);
        }
        getObjetoNegocio().setIdUsuario(idUsuario);
        boolean esInserccion;
        try {
            if (esNuevo(entidad)) {
                esInserccion = true;
                entidad = getObjetoNegocio().insertar(entidad);
            } else {
                esInserccion = false;
                entidad = getObjetoNegocio().actualizar(entidad);

            }
        } catch (BusinessException e) {
            return enviarErroresNegocio(e);
        } catch (Exception e) {
            return enviarErrorInesperado();
        }

        if (esInserccion) {
            return enviarInserccionExitosa(entidad);
        }
        return enviarModificacionExitosa(entidad);
    }

    protected Integer convertirIdAEntero(String cadena) {
        return new Integer(cadena);
    }

    protected void preparPlantillaAntesDeEnviar() {

    }
    
    /***
     * Carga las áreas a la plantilla para solicitar un formulario dasométrico
     */
    protected void cargarAreasAPlantillaFormularios(){
        CellRangeAddressList celdaArea = new CellRangeAddressList(4, 4, 2, 2);
        List<Area> areas = FactoriaObjetosNegocio.getInstance().getAreaBO().obtenerTodos();
        String[] codigos = new String[areas.size()];
        for (int i = 0; i < areas.size(); i++) {
            codigos[i] = areas.get(i).getCodigo();
        }
        DataValidationHelper dvHelper = hojaActual.getDataValidationHelper();
        DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(codigos);
        DataValidation validation = dvHelper.createValidation(dvConstraint, celdaArea);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        hojaActual.addValidationData(validation);
    }

    protected void setValorCelda(int rowIndex, int colIndex, Integer valor) {
        Cell celda = getCelda(rowIndex, colIndex);
        if (valor != null) {
            celda.setCellValue(valor);
        }
    }

    protected void setValorCelda(int rowIndex, int colIndex, String valor) {
        Cell celda = getCelda(rowIndex, colIndex);
        if (valor != null) {
            celda.setCellValue(valor);
        }
    }

    protected void setValorCelda(int rowIndex, int colIndex, Float valor) {
        Cell celda = getCelda(rowIndex, colIndex);
        if (valor != null) {
            celda.setCellValue(valor);
        }
    }

    protected void setValorCelda(int rowIndex, int colIndex, Short valor) {
        Cell celda = getCelda(rowIndex, colIndex);
        if (valor != null) {
            celda.setCellValue(valor);
        }
    }

    protected void setValorCelda(int rowIndex, int colIndex, Date valor) {
        Cell celda = getCelda(rowIndex, colIndex);
        if (valor != null) {
            celda.setCellValue(valor);
        }
    }

    protected void setValorCelda(int rowIndex, int colIndex, Double valor) {
        Cell celda = getCelda(rowIndex, colIndex);
        if (valor != null) {
            celda.setCellValue(valor);
        }
    }

    protected void setValorCelda(int rowIndex, int colIndex, Byte valor) {
        Cell celda = getCelda(rowIndex, colIndex);
        if (valor != null) {
            celda.setCellValue(valor);
        }
    }

    protected String getValorCeldaCadena(Cell celda) {
        return InterpretadorMensajeGenerico.getValorCelda(celda);
    }

    /***
     * Devuelve el valor de la celda en cadena
     * @param celda La celda que contien el valor
     * @return El valor de la celda en cadena, si la celda es nula, devuelve una cadena vacía
     */
    public static String getValorCelda(Cell celda) {
        if (celda == null){
            return "";
        }
        switch (celda.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                return celda.getStringCellValue();
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(celda.getBooleanCellValue());
            case Cell.CELL_TYPE_NUMERIC:
                double valor = celda.getNumericCellValue();
                if (valor % 1 == 0) {
                    return String.valueOf((int) valor);
                }
                return String.valueOf(valor);
            case Cell.CELL_TYPE_FORMULA:
            case Cell.CELL_TYPE_ERROR:
            case Cell.CELL_TYPE_BLANK:
            default:
                return "";
        }
    }

    abstract T convertirHojaEnEntidad();

    abstract BO getObjetoNegocio();

    abstract boolean esNuevo(T entidad);

    abstract String getId(T entidad);

    abstract ID convertirId(String cadena) throws Exception;

    abstract void mostrarLista(List<T> lista);

    abstract void mostrarEntidad(T entidad);

}
