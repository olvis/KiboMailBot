/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package bo.com.kibo.mailbot.intf;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import org.apache.poi.ss.usermodel.Sheet;

/**
 *
 * @author Olvinho
 */
public interface IInterpretadorMensaje {
    
    void setIdUsuario(Integer idUsuario);
    
    void setParametros(String parametro);
   
    void setNombreEntidad(String nombre);
    
    Multipart interpretar()throws MessagingException, IOException;
    
    List<File> obtenerArchivoTemporalesCreados();
    
    Multipart interpretarHojaExcel(Sheet hojaExcel)throws MessagingException;
    
}
