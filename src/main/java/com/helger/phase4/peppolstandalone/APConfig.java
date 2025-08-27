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
package com.helger.phase4.peppolstandalone;

import com.helger.annotation.concurrent.Immutable;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phase4.config.AS4Configuration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Immutable
public final class APConfig
{
  private APConfig ()
  {}

  @Nonnull
  public static IConfigWithFallback getConfig ()
  {
    return AS4Configuration.getConfig ();
  }

  @Nonnull
  public static EPeppolNetwork getPeppolStage ()
  {
    final String sStageID = getConfig ().getAsString ("peppol.stage");
    final EPeppolNetwork ret = EPeppolNetwork.getFromIDOrNull (sStageID);
    if (ret == null)
      throw new IllegalStateException ("Failed to determine peppol stage from value '" + sStageID + "'");
    return ret;
  }

  @Nullable
  public static String getMyPeppolSeatID ()
  {
    return getConfig ().getAsString ("peppol.seatid");
  }

  @Nullable
  public static String getMySmpUrl ()
  {
    return getConfig ().getAsString ("smp.url");
  }

  @Nullable
  public static String getPhase4ApiRequiredToken ()
  {
    return getConfig ().getAsString ("phase4.api.requiredtoken");
  }

  @Nullable
  public static String getMyPeppolCountryCode ()
  {
    return getConfig ().getAsString ("peppol.owner.countrycode");
  }

  @Nullable
  public static String getMyPeppolReportingSenderID ()
  {
    return getConfig ().getAsString ("peppol.reporting.senderid");
  }

  public static boolean isSchedulePeppolReporting ()
  {
    return getConfig ().getAsBoolean ("peppol.reporting.scheduled", true);
  }
}
