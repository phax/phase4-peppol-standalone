/*
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
package com.mysupply.phase4.peppolstandalone.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST Controller exception mapping to HTTP 500 (Internal Server Error)
 *
 * @author Philip Helger
 */
@ResponseStatus (HttpStatus.INTERNAL_SERVER_ERROR)
public class HttpInternalServerErrorException extends RuntimeException
{
  public HttpInternalServerErrorException (final String sMsg)
  {
    super (sMsg);
  }
}
