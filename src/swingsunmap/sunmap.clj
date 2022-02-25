(ns swingsunmap.sunmap)
;; Prepare a sun map from current time, daytime, nighttime maps.
;;  Started 2017Jun02


;; from MathUtil
(defn cos_deg "" [deg] (Math/cos (* deg (/ Math/PI 180.0))))
(defn ifloor "" [x]     (int (Math/floor x)) )
(defn interpolate "" [ x0 x x1 y0 y1]
  (cond
   (= x0 x1)  y0
   :else     (+ y0 (/ (* (- x x0) (- y1 y0)) (- x1 x0)) ) )    )

(defn iround "" [x]     (int (Math/round x)) )
(defn rmod   "" [ x y]  (- x (* (Math/floor (/ x y)) y) )  )
(defn sin_deg "" [deg] (Math/sin (* deg (/ Math/PI 180.0))))


;; from o.s.m.Angle  -- orig. 1 member
(defn atan2_nonneg "" [ y x]  (rmod (Math/atan2 y x) (* Math/PI 2.0)))


;; from o.s.m.SphericalPosition3D
(defn convertRectangular "" [x y z]
  {:longitude (atan2_nonneg y x)
   :latitude  (Math/atan2 z (Math/sqrt (+(* x x) (* y y))))
   :radius    (Math/sqrt (+ (* x x) (* y y) (* z z))) } )  ;; SP3D as map

(def D2Rad (/ Math/PI 180.0))          (def R2Deg (/ 180.0 Math/PI))

(defn getLongitudeDeg "" [pos]  (* R2Deg (:longitude pos)) )
(defn getLatitudeDeg  "" [pos]  (* R2Deg (:latitude  pos)) )
(defn getRightAscensionDeg "" [pos]  (getLongitudeDeg pos) )
(defn getDeclinationDeg "" [pos]     (getLatitudeDeg  pos) )


;; from o.s.a.Ecliptic  -- orig. 0 members; class only to permit cache?
(def JD_J2000 2451545.0)

(defn limitNegOneToOne "" [x]
  (cond (< x (- -1 0.01))  -1.0  ;; tolerance::0.01
        (> x (+  1 0.01))   1.0
        (< x -1.0)         -1.0
        (> x  1.0)          1.0
        :else               x )  )

(defn getNutation "subset -- MEAN_OBLIQUITY::1 only !  EV"  [timeJDE mode]
  (let [T      (/ (- timeJDE JD_J2000) 36525.0)
        coeff  [-4680.93 -1.55 1999.25 -51.38 -249.67 -39.05 7.12 27.87 5.79 2.45] ]
    (loop  [e 23.43929111    U (/ T 100.0)    i 0]
      (if (= (count coeff) i)
        [0.0  0.0  (* D2Rad e)]  ;; ret  [delta_psi  delta_epsilon  obliquity]
        (recur
         (+ e (/ (* (coeff i) U ) 3600.0) )
         (* U U)
         (inc i) ) ) )    )    ) ;; init test OK Sep06

(defn eclipticToEquatorial ""  [pos timeJDE mode]
  (let [nutation  (getNutation timeJDE mode)
        L         (:longitude pos)  ;; aka RightAscension
        B         (:latitude  pos)  ;; aka Declination
        E         (nutation 2)  ]   ;; 2::OBLIQUITY
    {:longitude (atan2_nonneg (- (* (Math/sin L) (Math/cos E))
                                 (* (Math/tan B) (Math/sin E)))
                            (Math/cos L) )
     :latitude  (Math/asin
                 (limitNegOneToOne (+ (* (Math/sin B) (Math/cos E))
                                      (* (Math/cos B) (Math/sin E) (Math/sin L))
                                      ) ) )
     :radius 0.0 }  )   )  ;; thus a SP3D  ;; init test OK Sep06


;; from o.s.a.SolarSystem
(defn getGreenwichMeanSiderealTime  ""  [ timeJDU]
  (let [t (- timeJDU JD_J2000)   T (/ t 36525)   T2 (* T T)   T3 (* T2 T)]
    (rmod
     (- (+ 280.46061837 (* 360.98564736629 t) (* 0.000387933 T2))
        (/ T3 38710000.0))
     360.0)  )    )

(defn getEclipticPosition
  "subset -- case planet:SUN flags:QUICK_SUN only! EV"  [planet timeJDE flags]
  (let [T   (/ (- timeJDE JD_J2000) 36525.0)
        T2  (* T T)
        e   (- 0.016708634 (* 0.000042037 T) (* 0.0000001267 T2) )
        L0  (+ 280.46646 (* 36000.76983 T) (* 0.0003032 T2) )
        M   (+ 357.52911 (* 35999.05029 T) (* -0.0001537 T2) )
        C   (+ (* (- 1.914602 (* 0.004817 T) (* 0.000014 T2)) (sin_deg M) )
               (* (- 0.019993 (* 0.000101 T))             (sin_deg (* 2.0 M)) )
               (* 0.000289  (sin_deg (* 3.0 M)) )   )
        L   (rmod (+ L0 C) 360.0)
        R   (/ (* 1.000001018 (- 1.0 (* e e)))
               (+ 1.0 (* e (cos_deg (+ M C)))) )  ]
    {:longitude (* D2Rad L)  :latitude 0.0  :radius R} )   ) ;; thus ret SP3D

(defn getEquatorialPosition  "subset for MEAN_OBLIQUITY::1 usecase "
  [planet timeJDE obs flags]
  (let [eclipticPos  (getEclipticPosition planet timeJDE  flags)
        pos          (eclipticToEquatorial eclipticPos timeJDE 1)]
    pos)
  )


;; from o.s.a.UTConvertor  (ala  jmoons-events)  table 2000..2014  calc to 2100

(defn hDT "ala historicDeltaT" [y]
  (let [iy   (int y)
        yx   (if (> iy 2014) 0  (- iy 2000))  ]
    ([63.82 64.09 64.30 64.47 64.57 64.68 64.84  65 65 66 66 66 67 69 71] yx)) )

(defn getDeltaTatJulianDate "" [ timeJDE]
  (let [year  (+ (/ (- timeJDE JD_J2000) 365.25) 2000.0)
        t     (/ (- year 2000) 100.0)
        dta   (+ 102.0 (* 102.0 t) (* 25.3 t t))
        dt3   (if (and (> year 2014) (< year 2100)) ;;off-end of table
                (+ dta (* 0.5161 (- year 2100.0)) )
                (hDT year) )  ]
   dt3)  )

(defn UT_to_TDB "" [timeJDU]
  (loop [timeJDE timeJDU   i 0]   ;;(println "timeJDE " timeJDE)
    (if (= i 5)
      timeJDE
      (recur  (+ timeJDU (/ (getDeltaTatJulianDate timeJDE) 86400.0))
              (inc i) ) ) )   )


(defn get-jd "convert date-time to julian (arity-overloaded)"
  ([v]
     (let [[y m d h mn s]  v]  (get-jd  y m d  h mn s)) )
  
  ([y m d h mn s]  ;; from J.Meeus via  T. Alonso Albi - OAN (Spain)
     (let [[Y M]  (if (< m 3)   [(dec y) (+ m 12)]   [y m] )
           A      (int (/ Y 100))
           B      (+ (- 2 A)  (/ A 4))
           dayFraction  (/ (+ h (/ (+ mn (/ s 60.0)) 60.0)) 24.0)
           jd     (+ dayFraction
                     (int (* 365.25 (+ Y 4716)))
                     (int (* 30.6001 (+ M 1)))
                     d  B  -1524.5)    ]
    jd) )    )     ;;Assert jd not in 2299150.0..2299160.0 !


;;from org.shetline.skyviewcafe.MapView  and ShadowedMap
(def imageWidth  800)        (def halfImageWidth  (/ imageWidth 2))
(def imageHeight 400)        (def halfImageHeight (/ imageHeight 2))
(def SUN 0)                  (def DAYLIGHT_EXPAND 1.0) ;;degrees
(def r  (cos_deg DAYLIGHT_EXPAND))     (def s_d_DE  (sin_deg DAYLIGHT_EXPAND))


(defn longit_to_x "" [ longit]
  (rmod (+ halfImageWidth (* (/ longit 360.0) imageWidth) ) imageWidth)  )


(defn m1inner "entire 'body' of original loop, including vector 'update'"
  [invec a msinLat cosLat x_adj z_adj sunLon]
  (let [sindeg_a (sin_deg a)
        cosdeg_a (cos_deg a)
        x   (+ (* msinLat sindeg_a r) x_adj)
        y   (* cosdeg_a r)
        z   (+ (* cosLat sindeg_a r) z_adj)
        pos (convertRectangular x y z)
        L   (getLongitudeDeg pos)        ;; aka daySpan below
        B   (getLatitudeDeg pos)
        yi  (max (min
                  (- halfImageHeight
                     (iround (* (/ B 180.0) imageHeight)))
                  (- imageHeight 1))
                 0)
        dayStart (longit_to_x (- sunLon L))
        dayEnd   (longit_to_x (+ sunLon L))
        iDayStart (ifloor dayStart)
        iDayend   (ifloor dayEnd)
        fiveTup  (if (> (Math/abs L) 0.1)
                   [dayStart dayEnd  L   iDayStart iDayend yi ]
                   [0        0       0   -1        0       yi ] )     ]
    (assoc invec yi fiveTup) )    )

(defn defaultrows "default vals with self-index as last!" []
  (loop [y 0   initvec (vec (repeat imageHeight [] )) ]
    (if (= y imageHeight)
      initvec
      (recur  (inc y) (assoc initvec y [0 0 0 -1 0 y] ) )  )   )   )

(defn m1 "" [ solarSystem timeJDU]
  (let [timeJDE (UT_to_TDB timeJDU) ; from UTConvertor
        pos0    (getEquatorialPosition SUN timeJDE 0 32) ;; 32==> QUICK_SUN
        sdrlTm  (getGreenwichMeanSiderealTime timeJDU)
        sunLon  (- (getRightAscensionDeg pos0) sdrlTm)
        sunLat  (getDeclinationDeg pos0)
        sinLat  (sin_deg sunLat)
        cosLat  (cos_deg sunLat)
        x_adj   (* s_d_DE (- 0 cosLat))
        msinLat (- 0 sinLat)
        z_adj   (* s_d_DE msinLat)
        initvec (defaultrows )  ] ;; self-index for e.g. '(frv3...)' !
    (loop [outvec initvec  a -90.0]
      (if (= a 91.0)
        outvec   ;; thus "return" outvec
        (recur (m1inner outvec a msinLat cosLat x_adj  z_adj sunLon)
               (inc a)) ) )    )  )


(defn interpolateLongitude "" [ x0 x x1 y0 y1]
  (let [y1a (cond
             (< y1 (- y0 halfImageWidth))  (+ y1 imageWidth)
             (> y1 (+ y0 halfImageWidth))  (- y1 imageWidth)
             :else                         y1)   ] 
    (rmod (interpolate x0 x x1 y0 y1a)  imageWidth) )  )


(defn m2inner "extr fn" [invec lastLat j i]
  (let [[dStartLL dEndLL dSpanLL unused1 unused2] (invec lastLat)
        [dStartI  dEndI  dSpanI  unused3 unused4] (invec i)
        dayStart  (interpolateLongitude lastLat j i dStartLL dStartI)
        dayEnd    (interpolateLongitude lastLat j i dEndLL   dEndI)
        ;_ (println "M2I"lastLat " "j " "i " " )
        daySpan   (interpolate          lastLat j i dSpanLL  dSpanI)   ]
    (assoc invec j
           [dayStart dayEnd daySpan (ifloor dayStart) (ifloor dayEnd) j ] ) )
  )


(defn NrPL [rows firstLat] (> ((rows firstLat) 2) ((rows (inc firstLat)) 2) ) )
(defn SoPL [rows lastLat ] (> ((rows lastLat)  2) ((rows (dec lastLat))  2) ) )


(defn m2bb "bridge the gap by interpolating as needed" [tLastLat i rowsv]
  (loop [j (inc tLastLat)   rows rowsv]
    (if (and (>= ((rows i)3) 0) (< j i) );;"natural" order, not a fix.
      (recur (inc j) (m2inner rows tLastLat j i))
      rows    ) ) )

(defn m2x "find and fill gaps" [ firstLat uLastLat rowsv]
  (loop [i (inc firstLat)  tLastLat firstLat  rows rowsv]
    (if (> i uLastLat)
      [rows (NrPL rows firstLat) (SoPL rows uLastLat)]
      (recur (inc i)
             (if (>= ((rows i)3) 0)  i  tLastLat) ;;must update to _i_ !!
             (m2bb tLastLat i rows)   )  )   )   )


(defn frv3 "" [northPoleLit southPoleLit  row]
  (let [[a b c daSt daEn y] row  equator halfImageHeight  imgW    imageWidth]
    (if (>= daSt 0)
      (if (< daSt daEn)  [y 0 (inc daSt)  y daEn (- imgW daEn) ] ;; 2 night seg
          [y daEn (- (inc daSt) daEn)           ] )              ;; 1
      (if (or (and (not northPoleLit) (< y equator))
              (and (not southPoleLit) (> y equator)) )
        [y 0 imgW] ;; full-width night line
        [] )    )) ;; empty ==>  full day line
  )


(defn getFrL "" [rows] (loop [i 0]   (if (> ((rows i)3) 0) i (recur (inc i)) )))
(defn getLsL "" [rows] (loop [i 399] (if (> ((rows i)3) 0) i (recur (dec i)) )))


(defn get-night-v "vector for painting night rows" [timeJD]
  (let [rws1            (m1 0 timeJD)
        [rows Nr So]    (m2x (getFrL rws1) (getLsL rws1) rws1)   ]
    (vec (map (partial frv3 Nr So ) rows) )    )  )


;;Note
;; Code with CamelCase names should be considered translation (or paraphrase)
;; of code from indicated packages/classes of "Sky View Cafe" which all
;; carry the following notice:


;;  Copyright (C) 2000-2007 by Kerry Shetline, kerry@shetline.com.

;;  This code is free for public use in any non-commercial application. All
;;  other uses are restricted without prior consent of the author, Kerry
;;  Shetline. The author assumes no liability for the suitability of this
;;  code in any application.

;;  2007 MAR 31   Initial release as Sky View Cafe 4.0.36.
