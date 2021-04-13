package ccc.interceptor;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.github.miemiedev.mybatis.paginator.OffsetLimitInterceptor;
import com.github.miemiedev.mybatis.paginator.dialect.Dialect;
import com.github.miemiedev.mybatis.paginator.domain.PageBounds;
import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.github.miemiedev.mybatis.paginator.domain.Paginator;
import com.github.miemiedev.mybatis.paginator.support.PropertiesHelper;
import com.github.miemiedev.mybatis.paginator.support.SQLHelp;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by kimsangyong on 2016. 5. 25..
 */
@Slf4j
@Intercepts({@Signature(
		type = Executor.class,
		method = "query",
		args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class CommonOffsetLimitInterceptor extends OffsetLimitInterceptor {

	static int MAPPED_STATEMENT_INDEX = 0;
	static int PARAMETER_INDEX = 1;
	static int ROWBOUNDS_INDEX = 2;
	static int RESULT_HANDLER_INDEX = 3;

	String mysqlDialect;

	String dialectClass;
	boolean asyncTotalCount = false;

	static ExecutorService Pool;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object intercept(final Invocation invocation) throws Throwable {
		final Executor executor = (Executor) invocation.getTarget();
		final Object[] queryArgs = invocation.getArgs();
		final MappedStatement ms = (MappedStatement) queryArgs[MAPPED_STATEMENT_INDEX];
		final Object parameter = queryArgs[PARAMETER_INDEX];
		final RowBounds rowBounds = (RowBounds) queryArgs[ROWBOUNDS_INDEX];
		final PageBounds pageBounds = new PageBounds(rowBounds);

		if (pageBounds.getOffset() == RowBounds.NO_ROW_OFFSET
				&& pageBounds.getLimit() == RowBounds.NO_ROW_LIMIT
				&& pageBounds.getOrders().isEmpty()) {
			return invocation.proceed();
		}

		final Dialect dialect;
		try {
			String dialectString = "";

			String databaseId = (new VendorDatabaseIdProvider()).getDatabaseId(ms.getConfiguration().getEnvironment().getDataSource());

			if (databaseId == null) databaseId = "";

			if (databaseId.toUpperCase().contains("MYSQL")) {
				dialectString = mysqlDialect;
			} else {
				dialectString = dialectClass;
			}
			Class clazz = Class.forName(dialectString);
			Constructor constructor = clazz.getConstructor(MappedStatement.class, Object.class, PageBounds.class);
			dialect = (Dialect) constructor.newInstance(new Object[]{ms, parameter, pageBounds});
		} catch (Exception e) {
			throw new ClassNotFoundException("Cannot create dialect instance: " + dialectClass, e);
		}

		final BoundSql boundSql = ms.getBoundSql(parameter);

		queryArgs[MAPPED_STATEMENT_INDEX] = copyFromNewSql(ms, boundSql, dialect.getPageSQL(), dialect.getParameterMappings(), dialect.getParameterObject());
		queryArgs[PARAMETER_INDEX] = dialect.getParameterObject();
		queryArgs[ROWBOUNDS_INDEX] = new RowBounds(RowBounds.NO_ROW_OFFSET, RowBounds.NO_ROW_LIMIT);

		Boolean async = pageBounds.getAsyncTotalCount() == null ? asyncTotalCount : pageBounds.getAsyncTotalCount();
		Future<List> listFuture = call(new Callable<List>() {
			public List call() throws Exception {
				return (List) invocation.proceed();
			}
		}, async);

		if (pageBounds.isContainsTotalCount()) {
			log.debug("offsetlimit ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼");
			log.debug(parameter.toString());
			Map<String, Object> paramMap = (Map<String, Object>) parameter;
			final Object parameters = (Object) paramMap;
			Callable<Paginator> countTask = new Callable() {
				public Object call() throws Exception {
					Integer count;
					Cache cache = ms.getCache();
					if (cache != null && ms.isUseCache() && ms.getConfiguration().isCacheEnabled()) {
						CacheKey cacheKey = executor.createCacheKey(ms, parameters, new PageBounds(), copyFromBoundSql(ms, boundSql, dialect.getCountSQL(), boundSql.getParameterMappings(), boundSql.getParameterObject()));
						count = (Integer) cache.getObject(cacheKey);
						if (count == null) {
							count = SQLHelp.getCount(ms, executor.getTransaction(), parameter, boundSql, dialect);
							cache.putObject(cacheKey, count);
						}
					} else {
						count = SQLHelp.getCount(ms, executor.getTransaction(), parameter, boundSql, dialect);
					}
					return new Paginator(pageBounds.getPage(), pageBounds.getLimit(), count);
				}
			};
			Future<Paginator> countFutrue = call(countTask, async);
			return new PageList(listFuture.get(), countFutrue.get());
		}

		return listFuture.get();
	}

	@Override
	public void setProperties(Properties properties) {
		PropertiesHelper propertiesHelper = new PropertiesHelper(properties);
		String dialectClass = propertiesHelper.getRequiredString("dialectClass");
		setDialectClass(dialectClass);

		String mysqlDialect = propertiesHelper.getRequiredString("mysqlDialect");

		setMysqlDialect(mysqlDialect);

		setAsyncTotalCount(propertiesHelper.getBoolean("asyncTotalCount", false));

		setPoolMaxSize(propertiesHelper.getInt("poolMaxSize", 0));

	}

	private MappedStatement copyFromNewSql(MappedStatement ms, BoundSql boundSql,
										   String sql, List<ParameterMapping> parameterMappings, Object parameter) {
		BoundSql newBoundSql = copyFromBoundSql(ms, boundSql, sql, parameterMappings, parameter);
		return copyFromMappedStatement(ms, new BoundSqlSqlSource(newBoundSql));
	}


	private BoundSql copyFromBoundSql(MappedStatement ms, BoundSql boundSql,
									  String sql, List<ParameterMapping> parameterMappings, Object parameter) {
		BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), sql, parameterMappings, parameter);
		for (ParameterMapping mapping : boundSql.getParameterMappings()) {
			String prop = mapping.getProperty();
			if (boundSql.hasAdditionalParameter(prop)) {
				newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
			}
		}
		return newBoundSql;
	}

	//see: MapperBuilderAssistant
	private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
		MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());

		builder.resource(ms.getResource());
		builder.fetchSize(ms.getFetchSize());
		builder.statementType(ms.getStatementType());
		builder.keyGenerator(ms.getKeyGenerator());
		if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
			StringBuffer keyProperties = new StringBuffer();
			for (String keyProperty : ms.getKeyProperties()) {
				keyProperties.append(keyProperty).append(",");
			}
			keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
			builder.keyProperty(keyProperties.toString());
		}

		//setStatementTimeout()
		builder.timeout(ms.getTimeout());

		//setStatementResultMap()
		builder.parameterMap(ms.getParameterMap());

		//setStatementResultMap()
		builder.resultMaps(ms.getResultMaps());
		builder.resultSetType(ms.getResultSetType());

		//setStatementCache()
		builder.cache(ms.getCache());
		builder.flushCacheRequired(ms.isFlushCacheRequired());
		builder.useCache(ms.isUseCache());

		return builder.build();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> Future<T> call(Callable callable, boolean async) {
		if (async) {
			return Pool.submit(callable);
		} else {
			FutureTask<T> future = new FutureTask(callable);
			future.run();
			return future;
		}
	}

	public void setDialectClass(String dialectClass) {
		log.debug("dialectClass: {} ", dialectClass);
		this.dialectClass = dialectClass;
	}

	public void setMysqlDialect(String mysqlDialect) {
		this.mysqlDialect = mysqlDialect;
	}
}
