/*
 * Copyright (C) 2023-204 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysupply.phase4.peppolstandalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan(basePackages = "com.mysupply.phase4")
@ComponentScan("com.helger")
@ComponentScan("com.mysupply.phase4.peppolstandalone.spi")
@EnableJpaRepositories(basePackages = "com.mysupply.phase4.persistence")
@EntityScan(basePackages = "com.mysupply.phase4.domain")
@SpringBootApplication
public class Phase4PeppolStandaloneApplication
{
  public static void main(final String [] args)
  {
    SpringApplication.run(Phase4PeppolStandaloneApplication.class, args);
  }
}
