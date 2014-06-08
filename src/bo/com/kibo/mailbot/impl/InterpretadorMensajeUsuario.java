/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IUsuarioBO;
import bo.com.kibo.entidades.Rol;
import bo.com.kibo.entidades.Usuario;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author Olvinho
 */
public class InterpretadorMensajeUsuario
        extends InterpretadorMensajeGenerico<Usuario, Integer, IUsuarioBO> {

    @Override
    Usuario convertirHojaEnEntidad() {
        Usuario entidad = new Usuario();
        Cell celda;
        //Id
        celda = getCelda(3, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }
        //Nombre
        celda = getCelda(4, 2);
        if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
            entidad.setNombre(getValorCeldaCadena(celda));
        }
        //Rol
        celda = getCelda(5, 2);
        if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
            entidad.setRol(new Rol());
            entidad.getRol().setDescripcion(getValorCeldaCadena(celda));
        }
        //Email
        celda = getCelda(6, 2);
        if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
            entidad.setEmail(getValorCeldaCadena(celda));
        }
        return entidad;
    }

    @Override
    protected void preparPlantillaAntesDeEnviar() {
        List<Rol> roles = FactoriaObjetosNegocio.getInstance().getRolBO().obtenerTodos();
        String[] descripciones = new String[roles.size()];
        for (int i = 0; i < roles.size(); i++) {
            descripciones[i] = roles.get(i).getDescripcion();
        }
        agregarValidacionLista(5, 5, 2, 2, descripciones, true, true);
    }

    @Override
    IUsuarioBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getIUsuarioBO();
    }

    @Override
    boolean esNuevo(Usuario entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(Usuario entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<Usuario> lista) {
        int i = 5;
        for (Usuario u : lista) {
            setValorCelda(i, 1, u.getId());
            setValorCelda(i, 2, u.getNombre());
            if (u.getRol() != null) {
                setValorCelda(i, 3, u.getRol().getDescripcion());
            }
            setValorCelda(i, 4, u.getEmail());
            i++;
        }
    }

    @Override
    void mostrarEntidad(Usuario entidad) {
        preparPlantillaAntesDeEnviar();
        setValorCelda(3, 2, entidad.getId());
        setValorCelda(4, 2, entidad.getNombre());
        if (entidad.getRol() != null){
            setValorCelda(5, 2, entidad.getRol().getDescripcion());
        }
        setValorCelda(6, 2, entidad.getEmail());
    }

    @Override
    protected void postInsertar(Usuario entidad) {
        //Enviar email al usuario
        String datosUsuario = "Usuario: " + entidad.getNombre() + "\n Contrasena: " + entidad.getContrasenaDesencriptada();
        System.out.println(datosUsuario);

    }
}
