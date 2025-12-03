package com.guicedee.telemetry.implementations;

import com.guicedee.client.services.config.IGuiceScanModuleInclusions;

import java.util.HashSet;
import java.util.Set;

public class GuiceTelemetryModuleInclusions
		implements IGuiceScanModuleInclusions<GuiceTelemetryModuleInclusions>
{
	@Override
	public Set<String> includeModules()
	{
		Set<String> strings = new HashSet<>();
		strings.add("com.guicedee.telemetry");
		return strings;
	}

}
