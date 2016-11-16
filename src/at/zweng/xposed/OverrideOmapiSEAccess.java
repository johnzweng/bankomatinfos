package at.zweng.xposed;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.newInstance;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * This is a module for the Xposed framework (http://repo.xposed.info/), whoch
 * ovverrides the Access Control for the Secure Element in the OpenMobile API
 * and allows one specified application fullö access to the SE. At the moment
 * the package name for which full access is granted is hardcoded (see
 * "TARGET_APPLICATION_PACKAGE_NAME" below).<br>
 * <br>
 * TODO: Make a UI and allow package name/app selction by user interface <br>
 * <br>
 * <br>
 * Licensed under GPL-3 (http://www.gnu.org/licenses/gpl-3.0.en.html)
 * 
 * @author Johannes Zweng, <john@zweng.at>, 13.12.2015
 */
public class OverrideOmapiSEAccess implements IXposedHookLoadPackage {
	private final static String OPENMOBILE_SERVICE = "org.simalliance.openmobileapi.service";
	private final static String LOG_PREFIX = "Bankomat_Card_Infos_2: ";

	//
	// =========================================================
	// Hardcoded package name goes here:
	// =========================================================
	//
	/**
	 * CHANGE THIS to the PACKAGE NAME of YOUR APP you which to grant full
	 * access to the SE. Any app with THIS PACKAGE NAME will be granted FULL
	 * ACCESS TO THE SECURE ELEMENT!<br>
	 * TODO: make configurable via GUI
	 */
	private final static String TARGET_APPLICATION_PACKAGE_NAME = "at.zweng.bankomatinfos2";
	//
	// =========================================================
	// =========================================================
	//

	/**
	 * Hook the method at package load time.
	 */
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if (!OPENMOBILE_SERVICE.equals(lpparam.packageName)) {
			// ignore loading of all other packages
			return;
		}
		XposedBridge
				.log(LOG_PREFIX + "we are in " + OPENMOBILE_SERVICE + " application. :-) Will place my method hooks.");

		// 1) try to find class:
		Class<?> accessControlEnforcerCls;
		try {
			accessControlEnforcerCls = findClass("org.simalliance.openmobileapi.service.security.AccessControlEnforcer",
					lpparam.classLoader);
		} catch (ClassNotFoundError e) {
			XposedBridge
					.log(LOG_PREFIX + "Could not find class 'AccessControlEnforcer' for hooking. :-( Will do NOTHING.");
			return;
		}

		// 2) try to locate one of the methods:
		Method methodSetupChannelAccess = null;
		
		// first variant as used in OMAPI 3.x:
		try {
			methodSetupChannelAccess = findMethodExact(accessControlEnforcerCls, "setUpChannelAccess", byte[].class,
					String.class);
		} catch (NoSuchMethodError nsme) {
			XposedBridge
					.log(LOG_PREFIX + "setUpChannelAccess() with 2 arguments (as used in OMAPI v3.x) was not found.");
		}
		
		// second variant as used in OMAPI 2.x:
		try {
			methodSetupChannelAccess = findMethodExact(accessControlEnforcerCls, "setUpChannelAccess", byte[].class,
					String.class, "org.simalliance.openmobileapi.service.ISmartcardServiceCallback");
		} catch (NoSuchMethodError nsme) {
			XposedBridge
					.log(LOG_PREFIX + "setUpChannelAccess() with 3 arguments (as used in OMAPI v2.x) was not found.");
		}
		
		// third variant as seen on a LeEco LePro 3 (LEX720) device, running EUI 5.8.018S:
		try {
			methodSetupChannelAccess = findMethodExact(accessControlEnforcerCls, "setUpChannelAccess", byte[].class,
					String.class, boolean.class, "org.simalliance.openmobileapi.service.ISmartcardServiceCallback");
		} catch (NoSuchMethodError nsme) {
			XposedBridge
					.log(LOG_PREFIX + "setUpChannelAccess() with 4 parameters (as seen in LeEco EUI 5.8) was not found.");
		}
		
		// if method not found, lets exit
		if (methodSetupChannelAccess == null) {
			XposedBridge
					.log(LOG_PREFIX + "Could not find method setUpChannelAccess() for hooking. :-( Sorry, can do NOTHING.");
			return;
		} else {
			XposedBridge.log(
					LOG_PREFIX + "Success! Found the following method to hook: " + methodSetupChannelAccess.toGenericString());
		}

		// hook the method:
		XposedBridge.hookMethod(methodSetupChannelAccess, setupChannelAccessHook);

		// log succesful hooking! :-)
		XposedBridge.log(LOG_PREFIX + "setUpChannelAccess() method hook in place! Let the fun begin! :-)");
	}

	/**
	 * Hook the method setupChannelAccess:<br>
	 * <br>
	 * public ChannelAccess setUpChannelAccess(<br>
	 * byte[] aid, String packageName, ISmartcardServiceCallback callback )
	 * 
	 */
	private final XC_MethodHook setupChannelAccessHook = new XC_MethodHook() {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
			try {
				if (param.args.length < 2) {
					XposedBridge.log(LOG_PREFIX
							+ "ERROR: setUpChannelAccess() was called but had less than 2 arguments. Will do nothing.");
					return;
				}
				if (!(param.args[1] instanceof String)) {
					XposedBridge.log(LOG_PREFIX
							+ "ERROR: setUpChannelAccess() was called but arg2 was not of type String. Will do nothing.");
					return;
				}
				String packageName = (String) param.args[1];
				if (!TARGET_APPLICATION_PACKAGE_NAME.equals(packageName)) {
					// not our target. Do nothing.
					XposedBridge.log(LOG_PREFIX + "setUpChannelAccess(): package '" + packageName
							+ "' doesn't match our target. Will not interfere with AccessRules.");
					return;
				}

				// OK, if we are here, the calling package name is the one we
				// want to grant full access
				Object channelAccessFullAccess = getFullAccessObject(param, packageName);
				XposedBridge.log(LOG_PREFIX + "setUpChannelAccess(): setting return value to this: "
						+ channelAccessFullAccess.toString());
				param.setResult(channelAccessFullAccess);
			} catch (Exception e) {
				XposedBridge.log(LOG_PREFIX + "ERROR in beforeHookedMethod of 'setUpChannelAccess()': " + e + ": "
						+ e.getMessage() + "\n" + getStackTraceAsString(e));
			}
		}
	};

	/**
	 * Get a "org.simalliance.openmobileapi.service.security.ChannelAccess"
	 * object which is setup to grant full access.
	 * 
	 * @return ChannelAccess with full access
	 */
	private static Object getFullAccessObject(MethodHookParam param, String packageName) throws Exception {
		// getClassloader
		ClassLoader clloader = param.thisObject.getClass().getClassLoader();
		Class<?> channelAccessClass = findClass("org.simalliance.openmobileapi.service.security.ChannelAccess",
				clloader);

		// create new instance
		Object channelAccess = newInstance(channelAccessClass);

		// get inner enum class:
		// public enum ACCESS { ALLOWED, DENIED, UNDEFINED; }
		Class<?> channelAccessEnumCls = findClass("org.simalliance.openmobileapi.service.security.ChannelAccess$ACCESS",
				clloader);

		// locate the 3 interesting methods for defining access
		Method setApduAccessMethod = findMethodExact(channelAccessClass, "setApduAccess", channelAccessEnumCls);
		Method setNFCEventAccessMethod = findMethodExact(channelAccessClass, "setNFCEventAccess", channelAccessEnumCls);
		Method setAccessMethod = findMethodExact(channelAccessClass, "setAccess", channelAccessEnumCls, String.class);
		// plus the one for setting package name
		Method setPackageName = findMethodExact(channelAccessClass, "setPackageName", String.class);

		// get the enum value "ALLOWED" (from enum ChannelAccess.ACCESS)
		Object[] enumConstants = channelAccessEnumCls.getEnumConstants();
		Object valueALLOWED = null;
		for (Object obj : enumConstants) {
			if ("ALLOWED".equals(obj.toString())) {
				valueALLOWED = obj;
				break;
			}
		}
		if (valueALLOWED == null) {
			throw new RuntimeException("Error: Could not find enum value ChannelAccess.ACCESS 'ALLOWED'.");
		}

		// finally call the 3 instance methods and set all 3 to "ACCESS"
		setApduAccessMethod.invoke(channelAccess, valueALLOWED);
		setNFCEventAccessMethod.invoke(channelAccess, valueALLOWED);
		setAccessMethod.invoke(channelAccess, valueALLOWED, "Full access allowed by Xposed. :-)");
		// and set package name (not sure if this is realy necessary)
		setPackageName.invoke(channelAccess, packageName);

		return channelAccess;
	}

	/**
	 * Get stack trace as string
	 * 
	 * @param e
	 * @return
	 */
	private static String getStackTraceAsString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
