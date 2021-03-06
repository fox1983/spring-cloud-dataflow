/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.completion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;

/**
 * Provides completions by finding apps whose name starts with a prefix (which was assumed
 * to be a correct app name, but wasn't).
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class UnfinishedAppNameExpansionStrategy implements ExpansionStrategy {

	private final AppRegistryService appRegistry;

	UnfinishedAppNameExpansionStrategy(AppRegistryService appRegistry) {
		this.appRegistry = appRegistry;
	}

	@Override
	public boolean addProposals(String text, StreamDefinition streamDefinition, int detailLevel,
			List<CompletionProposal> collector) {

		StreamAppDefinition lastApp = streamDefinition.getDeploymentOrderIterator().next();
		Set<String> parameterNames = new HashSet<>(lastApp.getProperties().keySet());
		parameterNames.removeAll(CompletionUtils.IMPLICIT_PARAMETER_NAMES);
		if (!parameterNames.isEmpty() || !text.endsWith(lastApp.getName())) {
			return false;
		}

		// Actually add completions

		String alreadyTyped = lastApp.getName();
		CompletionProposal.Factory proposals = CompletionProposal.expanding(text);

		List<ApplicationType> validTypesAtThisPosition = Arrays
				.asList(CompletionUtils.determinePotentialTypes(lastApp, streamDefinition.getAppDefinitions().size() > 1));

		for (AppRegistration appRegistration : appRegistry.findAll()) {
			String candidateName = appRegistration.getName();
			if (validTypesAtThisPosition.contains(appRegistration.getType()) && !alreadyTyped.equals(candidateName)
					&& candidateName.startsWith(alreadyTyped)) {
				String expansion = CompletionUtils.maybeQualifyWithLabel(appRegistration.getName(), streamDefinition);

				collector.add(proposals.withSuffix(expansion.substring(alreadyTyped.length())));
			}
		}
		return false;

	}
}
