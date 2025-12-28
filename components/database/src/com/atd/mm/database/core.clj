(ns com.atd.mm.database.core
  (:require [xtdb.node :as xt.node]
            [xtdb.api :as xt]))

(defn start-xtdb-node
  [config]
  (xt.node/start-node config))

(defn stop-xtdb-node
  [node]
  (.close node))

(comment
  (def my-node (xt.node/start-node {:log [:local
                                          {;; -- required
                                           ;; accepts `String`, `File` or `Path`
                                           :path "./tmp/log"

                                           ;; -- optional

                                           ;; accepts `java.time.InstantSource`
                                           ;; :instant-src (InstantSource/system)

                                           ;; :buffer-size 4096
                                           ;; :poll-sleep-duration "PT1S"
                                           }]}))

  (.close my-node)

  (xt/status my-node)

  (def my-persons
    [{:person/name "James Cameron",
      :person/born #inst "1954-08-16T00:00:00.000-00:00",
      :xt/id 100}
     {:person/name "Arnold Schwarzenegger",
      :person/born #inst "1947-07-30T00:00:00.000-00:00",
      :xt/id 101}
     {:person/name "Linda Hamilton",
      :person/born #inst "1956-09-26T00:00:00.000-00:00",
      :xt/id 102}
     {:person/name "Michael Biehn",
      :person/born #inst "1956-07-31T00:00:00.000-00:00",
      :xt/id 103}
     {:person/name "Ted Kotcheff",
      :person/born #inst "1931-04-07T00:00:00.000-00:00",
      :xt/id 104}
     {:person/name "Sylvester Stallone",
      :person/born #inst "1946-07-06T00:00:00.000-00:00",
      :xt/id 105}
     {:person/name "Richard Crenna",
      :person/born #inst "1926-11-30T00:00:00.000-00:00",
      :person/death #inst "2003-01-17T00:00:00.000-00:00",
      :xt/id 106}
     {:person/name "Brian Dennehy",
      :person/born #inst "1938-07-09T00:00:00.000-00:00",
      :xt/id 107}
     {:person/name "John McTiernan",
      :person/born #inst "1951-01-08T00:00:00.000-00:00",
      :xt/id 108}
     {:person/name "Elpidia Carrillo",
      :person/born #inst "1961-08-16T00:00:00.000-00:00",
      :xt/id 109}
     {:person/name "Carl Weathers",
      :person/born #inst "1948-01-14T00:00:00.000-00:00",
      :xt/id 110}
     {:person/name "Richard Donner",
      :person/born #inst "1930-04-24T00:00:00.000-00:00",
      :xt/id 111}
     {:person/name "Mel Gibson",
      :person/born #inst "1956-01-03T00:00:00.000-00:00",
      :xt/id 112}
     {:person/name "Danny Glover",
      :person/born #inst "1946-07-22T00:00:00.000-00:00",
      :xt/id 113}
     {:person/name "Gary Busey",
      :person/born #inst "1944-07-29T00:00:00.000-00:00",
      :xt/id 114}
     {:person/name "Paul Verhoeven",
      :person/born #inst "1938-07-18T00:00:00.000-00:00",
      :xt/id 115}
     {:person/name "Peter Weller",
      :person/born #inst "1947-06-24T00:00:00.000-00:00",
      :xt/id 116}
     {:person/name "Nancy Allen",
      :person/born #inst "1950-06-24T00:00:00.000-00:00",
      :xt/id 117}
     {:person/name "Ronny Cox",
      :person/born #inst "1938-07-23T00:00:00.000-00:00",
      :xt/id 118}
     {:person/name "Mark L. Lester",
      :person/born #inst "1946-11-26T00:00:00.000-00:00",
      :xt/id 119}
     {:person/name "Rae Dawn Chong",
      :person/born #inst "1961-02-28T00:00:00.000-00:00",
      :xt/id 120}
     {:person/name "Alyssa Milano",
      :person/born #inst "1972-12-19T00:00:00.000-00:00",
      :xt/id 121}
     {:person/name "Bruce Willis",
      :person/born #inst "1955-03-19T00:00:00.000-00:00",
      :xt/id 122}
     {:person/name "Alan Rickman",
      :person/born #inst "1946-02-21T00:00:00.000-00:00",
      :xt/id 123}
     {:person/name "Alexander Godunov",
      :person/born #inst "1949-11-28T00:00:00.000-00:00",
      :person/death #inst "1995-05-18T00:00:00.000-00:00",
      :xt/id 124}
     {:person/name "Robert Patrick",
      :person/born #inst "1958-11-05T00:00:00.000-00:00",
      :xt/id 125}
     {:person/name "Edward Furlong",
      :person/born #inst "1977-08-02T00:00:00.000-00:00",
      :xt/id 126}
     {:person/name "Jonathan Mostow",
      :person/born #inst "1961-11-28T00:00:00.000-00:00",
      :xt/id 127}
     {:person/name "Nick Stahl",
      :person/born #inst "1979-12-05T00:00:00.000-00:00",
      :xt/id 128}
     {:person/name "Claire Danes",
      :person/born #inst "1979-04-12T00:00:00.000-00:00",
      :xt/id 129}
     {:person/name "George P. Cosmatos",
      :person/born #inst "1941-01-04T00:00:00.000-00:00",
      :person/death #inst "2005-04-19T00:00:00.000-00:00",
      :xt/id 130}
     {:person/name "Charles Napier",
      :person/born #inst "1936-04-12T00:00:00.000-00:00",
      :person/death #inst "2011-10-05T00:00:00.000-00:00",
      :xt/id 131}
     {:person/name "Peter MacDonald",
      :person/born #inst "1939-02-20T00:00:00.000-00:00"
      :xt/id 132}
     {:person/name "Marc de Jonge",
      :person/born #inst "1949-02-16T00:00:00.000-00:00",
      :person/death #inst "1996-06-06T00:00:00.000-00:00",
      :xt/id 133}
     {:person/name "Stephen Hopkins",
      :person/born #inst "1958-11-01T00:00:00.000-00:00"
      :xt/id 134}
     {:person/name "Ruben Blades",
      :person/born #inst "1948-07-16T00:00:00.000-00:00",
      :xt/id 135}
     {:person/name "Joe Pesci",
      :person/born #inst "1943-02-09T00:00:00.000-00:00",
      :xt/id 136}
     {:person/name "Ridley Scott",
      :person/born #inst "1937-11-30T00:00:00.000-00:00",
      :xt/id 137}
     {:person/name "Tom Skerritt",
      :person/born #inst "1933-08-25T00:00:00.000-00:00",
      :xt/id 138}
     {:person/name "Sigourney Weaver",
      :person/born #inst "1949-10-08T00:00:00.000-00:00",
      :xt/id 139}
     {:person/name "Veronica Cartwright",
      :person/born #inst "1949-04-20T00:00:00.000-00:00",
      :xt/id 140}
     {:person/name "Carrie Henn",
      :person/born #inst "1976-05-07T00:00:00.000-00:00"
      :xt/id 141}
     {:person/name "George Miller",
      :person/born #inst "1945-03-03T00:00:00.000-00:00",
      :xt/id 142}
     {:person/name "Steve Bisley",
      :person/born #inst "1951-12-26T00:00:00.000-00:00",
      :xt/id 143}
     {:person/name "Joanne Samuel",
      :person/born #inst "1957-08-05T00:00:00.000-00:00",
      :xt/id 144}
     {:person/name "Michael Preston",
      :person/born #inst "1938-05-14T00:00:00.000-00:00",
      :xt/id 145}
     {:person/name "Bruce Spence",
      :person/born #inst "1945-09-17T00:00:00.000-00:00",
      :xt/id 146}
     {:person/name "George Ogilvie",
      :person/born #inst "1931-03-05T00:00:00.000-00:00",
      :xt/id 147}
     {:person/name "Tina Turner",
      :person/born #inst "1939-11-26T00:00:00.000-00:00",
      :xt/id 148}
     {:person/name "Sophie Marceau",
      :person/born #inst "1966-11-17T00:00:00.000-00:00",
      :xt/id 149}])


  (def my-movies
    [{:movie/title "The Terminator",
      :movie/year 1984,
      :movie/director 100,
      :movie/cast [101 102 103],
      :movie/sequel 207,
      :xt/id 200}
     {:movie/title "First Blood",
      :movie/year 1982,
      :movie/director 104,
      :movie/cast [105 106 107],
      :movie/sequel 209,
      :xt/id 201}
     {:movie/title "Predator",
      :movie/year 1987,
      :movie/director 108,
      :movie/cast [101 109 110],
      :movie/sequel 211,
      :xt/id 202}
     {:movie/title "Lethal Weapon",
      :movie/year 1987,
      :movie/director 111,
      :movie/cast [112 113 114],
      :movie/sequel 212,
      :xt/id 203}
     {:movie/title "RoboCop",
      :movie/year 1987,
      :movie/director 115,
      :movie/cast [116 117 118],
      :xt/id 204}
     {:movie/title "Commando",
      :movie/year 1985,
      :movie/director 119,
      :movie/cast [101 120 121],
      :xt/id 205}
     {:movie/title "Die Hard",
      :movie/year 1988,
      :movie/director 108,
      :movie/cast [122 123 124],
      :xt/id 206}
     {:movie/title "Terminator 2: Judgment Day",
      :movie/year 1991,
      :movie/director 100,
      :movie/cast [101 102 125 126],
      :movie/sequel 208,
      :xt/id 207}
     {:movie/title "Terminator 3: Rise of the Machines",
      :movie/year 2003,
      :movie/director 127,
      :movie/cast [101 128 129],
      :xt/id 208}
     {:movie/title "Rambo: First Blood Part II",
      :movie/year 1985,
      :movie/director 130,
      :movie/cast [105 106 131],
      :movie/sequel 210,
      :xt/id 209}
     {:movie/title "Rambo III",
      :movie/year 1988,
      :movie/director 132,
      :movie/cast [105 106 133],
      :xt/id 210}
     {:movie/title "Predator 2",
      :movie/year 1990,
      :movie/director 134,
      :movie/cast [113 114 135],
      :xt/id 211}
     {:movie/title "Lethal Weapon 2",
      :movie/year 1989,
      :movie/director 111,
      :movie/cast [112 113 136],
      :movie/sequel 213,
      :xt/id 212}
     {:movie/title "Lethal Weapon 3",
      :movie/year 1992,
      :movie/director 111,
      :movie/cast [112 113 136],
      :xt/id 213}
     {:movie/title "Alien",
      :movie/year 1979,
      :movie/director 137,
      :movie/cast [138 139 140],
      :movie/sequel 215,
      :xt/id 214}
     {:movie/title "Aliens",
      :movie/year 1986,
      :movie/director 100,
      :movie/cast [139 141 103],
      :xt/id 215}
     {:movie/title "Mad Max",
      :movie/year 1979,
      :movie/director 142,
      :movie/cast [112 143 144],
      :movie/sequel 217,
      :xt/id 216}
     {:movie/title "Mad Max 2",
      :movie/year 1981,
      :movie/director 142,
      :movie/cast [112 145 146],
      :movie/sequel 218,
      :xt/id 217}
     {:movie/title "Mad Max Beyond Thunderdome",
      :movie/year 1985,
      :movie/director [142 147],
      :movie/cast [112 148],
      :xt/id 218}
     {:movie/title "Braveheart",
      :movie/year 1995,
      :movie/director [112],
      :movie/cast [112 149],
      :xt/id 219}])


  (xt/submit-tx my-node [(into [:put-docs :persons] my-persons)
                         (into [:put-docs :movies] my-movies)])

  (into [:put-docs :persons] my-persons)


  (xt/submit-tx my-node [[:put-docs :files {:xt/id 1
                                            :src "example.mp4"
                                            :size 10000
                                            :duration 1002021}]])

  (xt/q my-node '(from :files {:bind [*]
                               :for-valid-time :all-time}))


  (xt/q my-node '(from :movies [movie/title]))

  (def q (partial xt/q my-node))

  (q '(from :movies [movie/title]))

  (q "SELECT movies.movie$title FROM movies")

  (q '(from :persons [{:person/name "Ridley Scott"} xt/id]))

  (q '(-> (from :persons [person/name xt/id])
          (where (= person/name "Ridley Scott"))))

  (q '(-> (from :persons [person/name xt/id])
          (where (= person/name "Ridley Scott"))
          (return xt/id)))


  (q '(-> (unify (from :movies [{:movie/title "Lethal Weapon"} movie/cast])
                 (unnest {p movie/cast})
                 (from :persons [{:xt/id p} person/name]))
          (return person/name)))

  (q '(-> (from :movies [movie/title {:movie/year 1985}])
          (return movie/title)))


  (q '(-> (from :movies [{:movie/title "Alien"} movie/year])
          (return movie/year)))

  (q '(-> (unify (from :movies [movie/director {:movie/title "RoboCop"}])
                 (from :persons [person/name {:xt/id movie/director}]))
          (return person/name)))

  (q '(-> (unify
           (rel [{:digits [1 2 3 4] :xt/id 1 :match 1}
                 {:digits [1 3 4] :xt/id 2 :match 2}
                 {:digits [1 2 4] :xt/id 3 :match 3}] [match digits])
           (unnest {match digits}))
          #_(return match)))


  (q '(-> (rel [{:foo 1 :my-list [1 2 3]}] [foo my-list])
          (unnest {foo my-list})))
  ;;=> Execution error (Incorrect) at xtdb.error/incorrect (error.clj:25).
  ;;   Attribute in col spec must be keyword
  ;;   


  (q '(->
       (unify (from :persons [{:person/name "Arnold Schwarzenegger"} {:xt/id arnie}])
              (from :movies [movie/cast movie/director])
              (unnest {arnie movie/cast})
              (from :persons [{:xt/id movie/director} person/name])
              #_(from :persons [{:xt/id director} person/name]))
       (return person/name)))



  (q ['(fn [actor]
         (->
          (unify (from :persons [{:person/name actor} {:xt/id arnie}])
                 (from :movies [movie/cast movie/director])
                 (unnest {arnie movie/cast})
                 (from :persons [{:xt/id movie/director} person/name])
                 #_(from :persons [{:xt/id director} person/name]))
          (return person/name)))
      "Arnold Schwarzenegger"])

  (q ['(fn [director-actor-rel]
         (-> (unify (rel director-actor-rel [director actor])
                    (from :persons [{:xt/id d} {:person/name director}])
                    (from :persons [{:xt/id a} {:person/name actor}])
                    (from :movies [{:movie/director d} movie/cast movie/title])
                    (unnest {a movie/cast}))
             (return movie/title)))
      [{:director "James Cameron"
        :actor "Arnold Schwarzenegger"}]])

  (q ['(fn [directors]
         (-> (unify (rel directors [director])
                    (from :persons [{:xt/id p} {:person/name director}])
                    (from :movies [{:movie/director p} movie/title]))
             (return movie/title)))
      [{:director "James Cameron"}
       {:director "Ridley Scott"}]])


  (q ['(fn [year]
         (-> (from :movies [{:movie/year year} xt/id movie/title])
             (return movie/title)))
      1988])


  (q ['(fn [title-rel]
         (-> (unify (rel title-rel [title])
                    (from :movies [{:movie/title title} movie/title movie/year]))
             (return movie/year title)))
      [{:title "Lethal Weapon"}]])


  (q ['(fn [director-actor-rel]
         (-> (unify
              (rel director-actor-rel [director actor])
              (from :persons [{:xt/id d} {:person/name director}])
              (from :persons [{:xt/id a} {:person/name actor}])
              (from :movies [{:movie/director d} movie/cast movie/title])
              (unnest {a movie/cast}))
             #_(return movie/title)))
      [{:director "James Cameron"
        :actor "Arnold Schwarzenegger"}]])

  (q '(-> (unify
           (rel {:son 10}))
          (return son)))


  (q ['(fn [name title-rating-rel]
         (->
          (unify
           (rel title-rating-rel [title rating])
           (from :persons [{:person/name name} {:xt/id a}])
           (from :movies [{:movie/title title} movie/cast])
           (unnest {a movie/cast}))
          #_(return title rating)))
      "Mel Gibson"
      [{:title "Die Hard", :rating 8.3}
       {:title "Alien", :rating 8.5}
       {:title "Lethal Weapon", :rating 7.6}
       {:title "Commando", :rating 6.5}
       {:title "Mad Max Beyond Thunderdome", :rating 6.1}
       {:title "Mad Max 2", :rating 7.6}
       {:title "Rambo: First Blood Part II", :rating 6.2}
       {:title "Braveheart", :rating 8.4}
       {:title "Terminator 2: Judgment Day", :rating 8.6}
       {:title "Predator 2", :rating 6.1}
       {:title "First Blood", :rating 7.6}
       {:title "Aliens", :rating 8.5}
       {:title "Terminator 3: Rise of the Machines", :rating 6.4}
       {:title "Rambo III", :rating 5.4}
       {:title "Mad Max", :rating 7.0}
       {:title "The Terminator", :rating 8.1}
       {:title "Lethal Weapon 2", :rating 7.1}
       {:title "Predator", :rating 7.8}
       {:title "Lethal Weapon 3", :rating 6.6}
       {:title "RoboCop", :rating 7.5}]])


  (q '(-> (from :movies [movie/title movie/year])
          (where (< movie/year 1984))))

  (q '(-> (from :persons [person/name])
          (where (like person/name "Ma%"))))


  ;; Q1. Find movies older than a certain year (inclusive)

  (q ['(fn [year]
         (-> (from :movies [movie/title movie/year])
             (where (> movie/year year))))
      1985])


  ;; Q2. Find actors older than Danny Glover
  (q ['(fn [actor]
         (->
          (unify (from :persons [{:person/name actor} {:person/born target-age}])
                 (from :persons [person/name person/born])
                 (where (> person/born target-age)))
          (return person/name)))
      "Elpidia Carrillo"])


  ;; Q1. Find movies older than a certain year (inclusive)

  (q ['(fn [year]
         (-> (from :movies [movie/year movie/title])
             (where (>= movie/year year))
             (return movie/title movie/year)))
      1991])



  ;; Q2. Find actors older than Danny Glover

  (q ['(fn [actor]
         (-> (unify
              (from :persons [{:person/name actor} {:person/born source-born}])
              (from :persons [person/name person/born])
              (where (> person/born source-born)))
             (return person/name)))
      "Claire Danes"])

  ;; Q3. Find movies newer than `year` (inclusive) and has a `rating` higher than the one supplied

  (q ['(fn [year rating title-rating-rel]
         (->
          (unify (rel title-rating-rel [{:rating r} title])
                 (from :movies [{:movie/title title} movie/year])
                 (where (>= movie/year year)
                        (> r rating)))
          (return title)))
      1990
      8.0
      [{:title "Die Hard", :rating 8.3}
       {:title "Alien", :rating 8.5}
       {:title "Lethal Weapon", :rating 7.6}
       {:title "Commando", :rating 6.5}
       {:title "Mad Max Beyond Thunderdome", :rating 6.1}
       {:title "Mad Max 2", :rating 7.6}
       {:title "Rambo: First Blood Part II", :rating 6.2}
       {:title "Braveheart", :rating 8.4}
       {:title "Terminator 2: Judgment Day", :rating 8.6}
       {:title "Predator 2", :rating 6.1}
       {:title "First Blood", :rating 7.6}
       {:title "Aliens", :rating 8.5}
       {:title "Terminator 3: Rise of the Machines", :rating 6.4}
       {:title "Rambo III", :rating 5.4}
       {:title "Mad Max", :rating 7.0}
       {:title "The Terminator", :rating 8.1}
       {:title "Lethal Weapon 2", :rating 7.1}
       {:title "Predator", :rating 7.8}
       {:title "Lethal Weapon 3", :rating 6.6}
       {:title "RoboCop", :rating 7.5}]])
  ;;=> [{:title "Braveheart"} {:title "Terminator 2: Judgment Day"}]

  (q '(->
       (from :movies [movie/title])
       (with {:hey (UPPER movie/title)})))


  ;;Keep from folding
  )