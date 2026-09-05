import { useEffect, useState, type CSSProperties } from "react";
import { formatSplashDate } from "./splashDate";

/** Minimum time before the skip hint may appear (skip itself waits only for appReady). */
export const SPLASH_SKIP_HINT_MIN_MS = 2000;

/** Three connected-cursive words; hollow outline only (approved layout). */
const FLOW_TITLE_WORDS = ["watch", "flow", "diary"] as const;

function renderFlowWord(word: (typeof FLOW_TITLE_WORDS)[number]) {
  if (word === "diary") {
    return (
      <>
        <span className="splash-title-lead">d</span>
        <span className="splash-title-rest">iary</span>
      </>
    );
  }

  return word;
}

function SplashFlowTitle() {
  return (
    <div className="splash-title-flow" aria-hidden="true">
      {FLOW_TITLE_WORDS.map((word) => (
        <span key={word} className={`splash-title-word splash-title-word--${word}`}>
          <span className="splash-title-word-outline">{renderFlowWord(word)}</span>
        </span>
      ))}
    </div>
  );
}

export function SplashScreen({
  onDismiss,
  appReady,
}: {
  onDismiss: () => void;
  appReady: boolean;
}) {
  const today = formatSplashDate(new Date());
  const [hintMinElapsed, setHintMinElapsed] = useState(false);
  const canSkip = appReady;
  const showHint = appReady && hintMinElapsed;

  useEffect(() => {
    const timer = window.setTimeout(() => setHintMinElapsed(true), SPLASH_SKIP_HINT_MIN_MS);
    return () => window.clearTimeout(timer);
  }, []);

  useEffect(() => {
    if (!canSkip) {
      return;
    }

    const dismiss = () => onDismiss();
    window.addEventListener("keydown", dismiss);
    return () => window.removeEventListener("keydown", dismiss);
  }, [onDismiss, canSkip]);

  return (
    <div
      className={`splash${canSkip ? " splash--skippable" : ""}`}
      role={canSkip ? "button" : "status"}
      tabIndex={canSkip ? 0 : undefined}
      aria-live="polite"
      aria-label={
        canSkip
          ? "WatchNest splash screen. Press any key or click to continue."
          : "WatchNest splash screen. Loading…"
      }
      onClick={canSkip ? onDismiss : undefined}
    >
      <div className="splash-inner">
        <div className="splash-content">
          <div className="splash-nest" aria-hidden="true">
            <span className="splash-nest-ring splash-nest-ring--1" />
            <span className="splash-nest-ring splash-nest-ring--2" />
            <span className="splash-nest-ring splash-nest-ring--3" />
            <span className="splash-nest-ring splash-nest-ring--4" />
            <span className="splash-nest-ring splash-nest-ring--5" />
            <span className="splash-nest-ring splash-nest-ring--6" />
            <span className="splash-nest-ring splash-nest-ring--7" />
            <span className="splash-nest-core" />
          </div>

          <p className="splash-eyebrow">WatchNest</p>
          <div className="splash-title-band">
            <h1 className="splash-title splash-title--flow" aria-label="Watch Flow Diary">
              <SplashFlowTitle />
            </h1>
          </div>

          <section className="splash-context" aria-label="Current date in week, month, and year">
            <article className="splash-context-block">
              <header className="splash-context-head">
                <span className="splash-context-label">Week</span>
                <span className="splash-context-meta">Week {today.isoWeek}</span>
              </header>
              <div className="splash-week-row">
                {today.weekDays.map((weekDay, index) => (
                  <div
                    key={`week-${index}`}
                    className={[
                      "splash-week-cell",
                      weekDay.isToday ? "is-today" : "",
                      weekDay.isCurrentMonth ? "" : "is-outside-month",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    <span className="splash-week-cell-day">{weekDay.short}</span>
                    <span className="splash-week-cell-num">{weekDay.day}</span>
                  </div>
                ))}
              </div>
            </article>

            <article className="splash-context-block">
              <header className="splash-context-head">
                <span className="splash-context-label">Month</span>
                <span className="splash-context-meta">
                  Day {today.day} of {today.daysInMonth}
                </span>
              </header>
              <div
                className="splash-month-track"
                style={{ "--days-in-month": today.daysInMonth } as CSSProperties}
                aria-hidden="true"
              >
                {today.monthDays.map((monthDay) => (
                  <span
                    key={monthDay.day}
                    className={[
                      "splash-month-day",
                      monthDay.isToday ? "is-today" : "",
                      !monthDay.isToday && monthDay.isPast ? "is-past" : "",
                      !monthDay.isToday && monthDay.isFuture ? "is-future" : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  />
                ))}
              </div>
              <p className="splash-context-caption">
                {today.month} {today.year}
              </p>
            </article>

            <article className="splash-context-block">
              <header className="splash-context-head">
                <span className="splash-context-label">Year</span>
                <span className="splash-context-meta">{today.year}</span>
              </header>
              <div className="splash-year-row">
                {today.yearMonths.map((month, index) => (
                  <div
                    key={`month-${index}`}
                    className={`splash-year-col splash-year-col--${month.season}`}
                  >
                    <span
                      className={[
                        "splash-year-month",
                        month.isCurrent ? "is-current" : "",
                        !month.isCurrent && month.isPast ? "is-past" : "",
                        !month.isCurrent && month.isFuture ? "is-future" : "",
                      ]
                        .filter(Boolean)
                        .join(" ")}
                    >
                      {month.short}
                    </span>
                    <div className="splash-year-month-remain-track" aria-hidden="true">
                      <span
                        className="splash-year-month-remain-fill"
                        style={{
                          width: `${(month.remainingDays / month.daysInMonth) * 100}%`,
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </article>
          </section>

          <p className="splash-date">
            <span className="splash-date-weekday">{today.weekday}</span>
            <span className="splash-date-detail">
              {today.day} {today.month} {today.year}
            </span>
          </p>

          <p className={`splash-skip-hint${showHint ? " is-visible" : ""}`}>
            Press any key or click to continue
          </p>
        </div>
      </div>
    </div>
  );
}
