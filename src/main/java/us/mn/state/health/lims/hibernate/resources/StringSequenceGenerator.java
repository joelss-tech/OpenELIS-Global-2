package us.mn.state.health.lims.hibernate.resources;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

public class StringSequenceGenerator extends SequenceStyleGenerator {
	private String numberFormat = "%d";

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {

		Long id = (Long) super.generate(session, object);
		return String.format(numberFormat, super.generate(session, object));
	}

	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		super.configure(LongType.INSTANCE, params, dialect);
	}

}