package com.guicedee.telemetry.implementations;

import com.guicedee.client.services.config.IGuiceScanModuleInclusions;

import java.util.HashSet;
import java.util.Set;

/**
 * Declares modules to include in the ClassGraph scan for telemetry.
 */
public class GuiceTelemetryModuleInclusions
		implements IGuiceScanModuleInclusions<GuiceTelemetryModuleInclusions>
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> includeModules()
	{
		Set<String> strings = new HashSet<>();
		strings.add("com.guicedee.telemetry");
		return strings;
	}

}
