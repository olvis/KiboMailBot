/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessExceptionMessage;
import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IEspecieBO;
import bo.com.kibo.entidades.Especie;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author Olvinho
 */
public class InterpretadorMensajeEspecie extends InterpretadorMensajeGenerico<Especie, Integer, IEspecieBO> {

    @Override
    Especie convertirHojaEnEntidad() {
        Especie entidad = new Especie();
        Cell celda;
        //Id
        celda = getCelda(3, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }

        //Nombre
        celda = getCelda(4, 2);
        entidad.setNombre(getValorCeldaCadena(celda));

        //Cientifico
        celda = getCelda(5, 2);
        entidad.setCientifico(getValorCeldaCadena(celda));

        //Factor
        celda = getCelda(6, 2);
        if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
            //No requerido
            if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                entidad.setFactor((float)celda.getNumericCellValue());
            } else {
                appendException(new BusinessExceptionMessage("El factor debe ser un número decimal"));
            }
        }

        //DMC
        celda = getCelda(7, 2);
        if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
            //No requerido
            if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                entidad.setDmc((float)celda.getNumericCellValue());
            } else {
                appendException(new BusinessExceptionMessage("El DMC debe ser un número decimal"));
            }
        }
        return entidad;
    }

    @Override
    IEspecieBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getEspecieBO();
    }

    @Override
    boolean esNuevo(Especie entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(Especie entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<Especie> lista) {
        int i = 5;
        for (Especie e : lista) {
            setValorCelda(i, 1, e.getId());
            setValorCelda(i, 2, e.getNombre());
            setValorCelda(i, 3, e.getCientifico());
            setValorCelda(i, 4, e.getFactor());
            setValorCelda(i, 5, e.getDmc());
            i++;
        }
    }

    @Override
    void mostrarEntidad(Especie entidad) {
        setValorCelda(3, 2, entidad.getId());
        setValorCelda(4, 2, entidad.getNombre());
        setValorCelda(5, 2, entidad.getCientifico());
        setValorCelda(6, 2, entidad.getFactor());
        setValorCelda(7, 2, entidad.getDmc());
    }

}
