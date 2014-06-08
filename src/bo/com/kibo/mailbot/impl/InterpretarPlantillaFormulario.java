/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessException;
import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.mailbot.intf.IInterpretadorFormularioDasometrico;
import bo.com.kibo.mailbot.intf.IInterpretadorMensaje;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 *
 * @author Olvinho
 */
public class InterpretarPlantillaFormulario implements IInterpretadorMensaje{

    private Integer idUsuario;
    private final List<File> archivosTemporales = new ArrayList<>();
    
    @Override
    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    @Override
    public void setParametros(String parametro) {
       // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNombreEntidad(String nombre) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Multipart interpretar() throws MessagingException, IOException {
        return null;
    }

    @Override
    public List<File> obtenerArchivoTemporalesCreados() {
        return archivosTemporales;
    }

    @Override
    public Multipart interpretarHojaExcel(Sheet hojaExcel) throws MessagingException {
        Cell celda;
        Row fila = hojaExcel.getRow(3);
        if (fila == null)
            return InterpretadorMensajeGenerico.enviarErroresNegocio(new BusinessException("Plantilla no válida"));
        celda = fila.getCell(2);
        String tipo = InterpretadorMensajeGenerico.getValorCelda(celda).toLowerCase();
        fila = hojaExcel.getRow(4);
        if (fila == null)
            return InterpretadorMensajeGenerico.enviarErroresNegocio(new BusinessException("Plantilla no válida"));
        celda = fila.getCell(2);
        String codigoArea = InterpretadorMensajeGenerico.getValorCelda(celda);
        if ("".equals(tipo) || "".equals(codigoArea)){
            return InterpretadorMensajeGenerico.enviarErroresNegocio(new BusinessException("Debe seleccionar tipo y área en la plantilla"));
        }
        IInterpretadorMensaje interprete = InterpretadorMensajeGenerico.getMapaObjetos().get(tipo);
        if ((interprete == null) || (!(interprete instanceof IInterpretadorFormularioDasometrico))){
            return InterpretadorMensajeGenerico.enviarErroresNegocio(new BusinessException("El tipo no es válido"));
        }
        
        Area area = FactoriaObjetosNegocio.getInstance().getAreaBO().recuperarPorCodigo(codigoArea);
        if (area == null){
            return InterpretadorMensajeGenerico.enviarErroresNegocio(new BusinessException("El área '" + codigoArea +"' no existe"));
        }
        
        interprete.setIdUsuario(idUsuario);
        interprete.setNombreEntidad(tipo);
        interprete.setParametros("plantilla");
        ((IInterpretadorFormularioDasometrico)interprete).setArea(area);
        ((InterpretadorMensajeGenerico)interprete).setCargarPlantillaFormularios(false);
        Multipart respueta;
        try {
            respueta = interprete.interpretar();
            this.archivosTemporales.addAll(interprete.obtenerArchivoTemporalesCreados());
        } catch (IOException ex) {
            return InterpretadorMensajeGenerico.enviarErroresNegocio(new BusinessException("Ocurrió un error inesperado, intente más tarde"));
        }
        ((InterpretadorMensajeGenerico)interprete).setCargarPlantillaFormularios(true);
        return respueta;
    }
    
   
    
}
