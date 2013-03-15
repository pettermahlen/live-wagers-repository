CREATE
  TABLE wager_round
  (
    wager_round_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    exchange_rate_id BIGINT NOT NULL,
    outcome_amount BIGINT,            -- will be null until outcome has been received from game
    outcome_timestamp TIMESTAMP,      -- will be null until outcome has been confirmed from money service
    archive_timestamp TIMESTAMP,      -- will be null until wager round has been archived
    PRIMARY KEY (wager_round_id)
  );

PARTITION TABLE wager_round ON COLUMN wager_round_id;

CREATE
  TABLE wager
  (
    wager_round_id BIGINT NOT NULL,
    wager_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    created TIMESTAMP NOT NULL,
    confirmed TIMESTAMP,              -- will be null until the wager has been confirmed
    PRIMARY KEY (wager_round_id, wager_id)
  );

PARTITION TABLE wager ON COLUMN wager_round_id;


-- Roles
CREATE ROLE system WITH sysproc,adhoc,defaultproc;
-- Not great in a way to allow sysproc for app, but it simplifies some things
CREATE ROLE app WITH sysproc;
CREATE ROLE test WITH sysproc,adhoc,defaultproc;

-- Procedures
CREATE PROCEDURE ALLOW app FROM CLASS com.williamsinteractive.casino.wager.procedures.RecordWager;
PARTITION PROCEDURE RecordWager ON TABLE wager_round COLUMN wager_round_id;

CREATE PROCEDURE ALLOW app FROM CLASS com.williamsinteractive.casino.wager.procedures.RecordOutcome;
PARTITION PROCEDURE RecordOutcome ON TABLE wager_round COLUMN wager_round_id;

CREATE PROCEDURE ALLOW app FROM CLASS com.williamsinteractive.casino.wager.procedures.RecordArchival;
PARTITION PROCEDURE RecordArchival ON TABLE wager_round COLUMN wager_round_id;

CREATE PROCEDURE WAGER_ROUND_SELECT_ALL ALLOW test AS SELECT * FROM wager_round;
CREATE PROCEDURE WAGER_SELECT_ALL ALLOW test AS SELECT * FROM wager;
