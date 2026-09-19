package com.github.erosb.quadrator;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ReconstitutionFactory<T> {

    T reconstitute(ResultSetAccessor rs) throws SQLException;
}
