package io.xream.x7.demo.aop;



import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import x7.core.bean.DataPermission;
import x7.config.SpringHelper;
import x7.core.util.ExceptionUtil;
import x7.core.util.TimeUtil;
import x7.core.web.Tokened;
import x7.core.web.ViewEntity;
import x7.repository.dao.Tx;

@Aspect
@Configuration
public class WebAop {

	private Logger logger = LoggerFactory.getLogger(WebAop.class);

	@Pointcut("execution(public * io.xream.x7.demo.controller.*.*(..))")
	public void cut() {

	}

	@Around("cut()")
	public Object around(ProceedingJoinPoint proceedingJoinPoint) {

		Object[] argArr = proceedingJoinPoint.getArgs();
//		Passport passport = null;
//		{
//			/*
//			 * isSignIn, FIXME 移到网关
//			 */
//			if (argArr != null) {
//				for (Object arg : argArr) {
//					if (arg instanceof Tokened) {
//						try {
//							passport = PassportUtilX.getPassport((Tokened) arg);
//							/*
//							 * 数据权限
//							 */
//							if (arg instanceof DataPermission) {
//								DataPermission.Chain.beforeHandle((DataPermission)arg, passport.getDataPermissionValue());
//							}
//							break;
//						} catch (Passport.PassportException e) {
//							Passport.PassportException pe = (Passport.PassportException) e;
//							return pe.getViewEntity();
//						}
//					}
//				}
//			}
//		}

		org.aspectj.lang.Signature signature = proceedingJoinPoint.getSignature();
		MethodSignature ms = ((MethodSignature) signature);
		final String mapping = SpringHelper.getRequestMapping(ms.getMethod());

		
		{
			/*
			 * TX
			 */
			logger.info("_______Transaction begin ....by request: " + mapping);
			long startTime = TimeUtil.now();
			Tx.begin();
			try {
				Object obj = null;

				Class returnType = ms.getReturnType();
				if (returnType == void.class) {

					proceedingJoinPoint.proceed();
				} else {
					obj = proceedingJoinPoint.proceed();
				}

				Tx.commit();
				long endTime = TimeUtil.now();
				long handledTimeMillis = endTime - startTime;
				logger.info("_______Transaction end, cost time: " + (handledTimeMillis) + "ms");
				if (obj instanceof ViewEntity){
					ViewEntity ve = (ViewEntity)obj;
					ve.setHandledTimeMillis(handledTimeMillis);
				}
				
				return obj;
			} catch (Throwable e) {
				e.printStackTrace();
				Tx.rollback();

//				if(e instanceof HystrixRuntimeException){
//					return ViewEntity.toast("服务繁忙, 请稍后");
//				}

				String msg = ExceptionUtil.getMessage(e);

				logger.info("_______GOT EXCEIPTION ....by request: " + mapping + "  ....:" + msg);
				logger.info("_______ROLL BACKED ....by request: " + mapping);

				return ViewEntity.toast(msg);
			}
		}
	}
}
