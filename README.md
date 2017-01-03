#JResume - The best JSON to HTML resume generator.

##Example usage

###Example JSON resume (example.json):

    {
      "person": {
        "name": "John Doe",
        "email": "johndoe@gmail.com",
        "address": "7 Java Drive, OOP City",
        "phoneNumber": "+1(334)567-2346",
        "jobTitle": "Software Engineer",
        "website": "https://www.google.com"
      },
      "skills": [
        {
          "name": "Java",
          "competence": "Intermediate"
        },
        {
          "name": "C++",
          "competence": "Beginner"
        },
        {
          "name": "Android",
          "competence": "Intermediate"
        }
      ],
      "jobWork": [
        {
          "company": "Example Ltd.",
          "position": "Software Engineer",
          "summary": "At Example Ltd., I did such and such and such and such and such and such and such and such and such.",
          "startDate": "August 19, 1997",
          "endDate": "August 19, 1990",
          "highlights": [
            "Worked on such and such",
            "Also worked on this"
          ],
          "keywords": [
            "java",
            "c++"
          ]
        },
        {
          "company": "Example Ltd.2",
          "position": "Software Engineer",
          "summary": "At Example Ltd.2, I did such and such and such and such and such and such and such and such and such.",
          "startDate": "August 19, 1997",
          "endDate": "August 19, 1990",
          "highlights": [
            "Worked on such and such",
            "Also worked on this"
          ],
          "keywords": [
            "java",
            "c++"
          ]
        }
      ],
      "volunteerWork": [
        {
          "company": "Example Institution",
          "position": "Volunteer",
          "summary": "At Example Institution, I did such and such.",
          "startDate": "August 19, 1997",
          "endDate": "August 19, 1990",
          "highlights": [
            "Worked on such and such",
            "Also worked on this"
          ],
          "keywords": [
            "java",
            "c++"
          ]
        },
        {
          "company": "Example Institution2",
          "position": "Volunteer",
          "summary": "At Example Institution2, I did such and such.",
          "startDate": "August 19, 1997",
          "endDate": "August 19, 1990",
          "highlights": [
            "Worked on such and such",
            "Also worked on this"
          ],
          "keywords": [
            "java",
            "c++"
          ]
        }
      ],
      "projects": [
        {
          "name": "AwesomeProject",
          "description": "This awesome project is awesome!",
          "highlights": [
            "Does such and such.",
            "And it does such and such."
          ],
          "keywords": [
            "java",
            "c++"
          ],
          "url": "https://www.github.com"
        },
        {
          "name": "AwesomeProject2",
          "description": "This awesome project2 is awesome!",
          "highlights": [
            "Does such and such.",
            "And it does such and such."
          ],
          "keywords": [
            "java",
            "c++"
          ],
          "url": "https://www.github.com"
        }
      ],

      "numSkillColumns": 3
    }

###Usage:

    java -jar jresume.jar --input example.json --output output --theme default

###Output:

![output](https://raw.githubusercontent.com/chenshuiluke/jresume/master/screenshots/default_theme_at_commit_9fe766ad23a192d2f4b833eb8623d558bd4443b1.png)