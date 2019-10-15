------------------------------------------------------------------------
------------------------------------------------------------------------
-------------------------- POUR CHAQUE SCHEMA --------------------------
------------------------------------------------------------------------
-- SET SCHEMA 'tun';                                                  --
-- SET SCHEMA 'tde';                                                  --
-- SET SCHEMA 'ttr';                                                  --
------------------------------------------------------------------------
------------------------------------------------------------------------
------------------------------------------------------------------------
------------------------------------------------------------------------


------------------------------------------------------------------------------------------------
------------------------- MAPPING HASTUS ZDEP (PAS POUR CHAQUE SCHEMA) -------------------------
------------------------------------------------------------------------------------------------
CREATE SEQUENCE public.mapping_hastus_zdep_id_seq
  INCREMENT 1
  MINVALUE 1
MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER TABLE public.mapping_hastus_zdep_id_seq OWNER TO chouette;
-- DROP TABLE public.mapping_hastus_zdep;
CREATE TABLE public.mapping_hastus_zdep(
  id              BIGINT PRIMARY KEY,
  referential     CHARACTER VARYING(50),
  zdep            CHARACTER VARYING(255),
  hastus_chouette CHARACTER VARYING(255),
  hastus_original CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX mapping_hastus_zdep_zdep_idx                 ON public.mapping_hastus_zdep (zdep);
CREATE        INDEX mapping_hastus_zdep_zdep_referential_idx     ON public.mapping_hastus_zdep (referential);
CREATE        INDEX mapping_hastus_zdep_zdep_hastus_chouette_idx ON public.mapping_hastus_zdep (hastus_chouette);
CREATE        INDEX mapping_hastus_zdep_zdep_hastus_original_idx ON public.mapping_hastus_zdep (hastus_original);
ALTER TABLE public.mapping_hastus_zdep OWNER TO chouette;

-------------------------------------------------------------------------------------------------------
------------------------- UPDATE STOP AREAS WITH ZDEP (PAS POUR CHAQUE SCHEMA) ------------------------
-------------------------------------------------------------------------------------------------------
ALTER TABLE public.stop_areas ADD mapping_hastus_zdep_id BIGINT;
ALTER TABLE ONLY public.stop_areas
    ADD CONSTRAINT sa_mapping_hastus_zdep_fk FOREIGN KEY (mapping_hastus_zdep_id) REFERENCES public.mapping_hastus_zdep(id);
--ALTER TABLE ONLY public.stop_areas DROP CONSTRAINT sa_mapping_hastus_zdep_fk;
-- UPDATE public.stop_areas SET mapping_hastus_zdep_id = NULL;


----------------------------------------------------------------------------------------------------------
------------------------- UPDATE IDFM STOP AREAS PUBLIC (PAS POUR CHAQUE SCHEMA) -------------------------
----------------------------------------------------------------------------------------------------------
-- DROP FUNCTION IF EXISTS public.update_sa_for_idfm_line(TEXT, BIGINT);
CREATE OR REPLACE FUNCTION public.update_sa_for_idfm_line(referential TEXT, lineid BIGINT) RETURNS BOOLEAN AS
$BODY$
DECLARE
  B  BOOLEAN;
  T  TEXT;
  N  NAME;
BEGIN
  SELECT * INTO N FROM current_schema();
  EXECUTE 'SET search_path TO ' || referential;
  B := FALSE;
  T := 'SELECT ' ||referential || '._update_sa_for_idfm_line(' || lineid::TEXT || ')';
  EXECUTE T INTO B;
  EXECUTE 'SET search_path TO ' || N;
  RETURN B;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION public.update_sa_for_idfm_line(TEXT, BIGINT)
  OWNER TO chouette;


--------------------------------------------------------------------------------------
------------------------- UPDATE IDFM STOP AREAS REFERENTIAL -------------------------
--------------------------------------------------------------------------------------

-------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> GFO  SELECT public.update_sa_for_idfm_line('tun', 15);

-- DROP FUNCTION IF EXISTS update_sa_for_idfm_line(BIGINT);
CREATE OR REPLACE FUNCTION _update_sa_for_idfm_line(lineid BIGINT) RETURNS BOOLEAN AS
$BODY$
DECLARE
  idPlage BIGINT;
  isIDFM  BOOLEAN;
  L1      RECORD;
  I       INTEGER;
  S       TEXT;
  BEGIN
  -- Ligne existe ?
  SELECT COUNT(*) INTO I FROM lines WHERE id = lineid;
  IF (I=0) THEN
    RAISE EXCEPTION 'La ligne % n''existe pas', lineid;
  END IF;

  -- Test si ligne IDFM
  isIDFM := FALSE;
  FOR L1 IN (SELECT cfl.* FROM lines l
               JOIN categories_for_lines cfl ON cfl.id = l.categories_for_line_id
              WHERE l.id = lineid
                AND UPPER(cfl.name) LIKE 'IDFM')
  LOOP
    isIDFM = TRUE;
    EXIT;
  END LOOP;

  FOR L1 IN (SELECT DISTINCT(sa.*) FROM public.stop_areas sa
              JOIN scheduled_stop_points ssp         ON ssp.stop_area_objectid_key = sa.objectid
              JOIN stop_points sp                    ON sp.scheduled_stop_point_id = ssp.id
              JOIN journey_patterns_stop_points jpsp ON jpsp.stop_point_id         = sp.id
              JOIN journey_patterns jp               ON jp.id                      = jpsp.journey_pattern_id
              JOIN routes r                          ON r.id                       = jp.route_id
             WHERE COALESCE(sa.mapping_hastus_zdep_id, 0) = 0
               AND line_id                                = lineid
             ORDER BY sa.id)
  LOOP
    IF (isIDFM) THEN -- test plus pertinent hors de la boucle car réalisé n fois
      SELECT current_schema INTO S FROM current_schema();
      SELECT MIN(id) INTO idPlage
        FROM public.mapping_hastus_zdep
       WHERE hastus_chouette IS NULL
         AND referential LIKE S;
      IF (idPlage IS NULL) THEN
        RAISE EXCEPTION 'La plage d''identifiants ZDEP est trop petite, il manque des valeurs pour le schema %', S;
      END IF;
      UPDATE public.mapping_hastus_zdep SET hastus_chouette        = L1.objectid WHERE id = idPlage;
      UPDATE public.stop_areas          SET mapping_hastus_zdep_id = idPlage     WHERE id = L1.id AND mapping_hastus_zdep_id IS NULL;
    ELSE
      UPDATE public.stop_areas SET mapping_hastus_zdep_id = NULL WHERE id = L1.id AND mapping_hastus_zdep_id IS NOT NULL;
    END IF;
  END LOOP;
  RETURN TRUE;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION _update_sa_for_idfm_line(BIGINT)
  OWNER TO chouette;

