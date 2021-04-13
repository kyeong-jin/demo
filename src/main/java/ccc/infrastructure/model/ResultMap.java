package ccc.infrastructure.model;

import java.sql.Clob;
import java.sql.SQLException;

import org.apache.commons.collections.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultMap extends ListOrderedMap {

	private static final long serialVersionUID = 5216692821589938114L;
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public Object put(Object key, Object value) {
		Object setValue = null;
		
		Class<?> dataType = value.getClass();
		if (dataType != null) {
			if(dataType.toString().toUpperCase().indexOf("CLOB") > -1) {
				try {
					Clob clob = (Clob) value;
					
					int size = (int) clob.length();
					setValue = (Object) clob.getSubString(1, size);
					
					value = setValue;
				} catch(SQLException se) {
					logger.error(se.toString());
				}
			}
		}
		return super.put(convert2CamelCase((String)key), value);
	}

	private String convert2CamelCase(String underScore) {
		
		if (underScore.indexOf('_') < 0 && Character.isLowerCase(underScore.charAt(0))) {
			return underScore;
		}
		StringBuilder result = new StringBuilder();
		boolean nextUpper = false;
		int len = 0;
		
		if (underScore != null) {
			len = underScore.length();
		}

		for (int i = 0; i < len; i++) {
			char currentChar = underScore.charAt(i);
			if (currentChar == '_') {
				nextUpper = true;
			} else {
				if (nextUpper) {
					result.append(Character.toUpperCase(currentChar));
					nextUpper = false;
				} else {
					result.append(Character.toLowerCase(currentChar));
				}
			}
		}
		return result.toString();
	}

}
