Feature: "commit" command
    In order to finalize a set of changes that have been staged
    As a Geogit User
    I want to create a commit and add it to the repository

  Scenario: Try to commit current staged features
    Given I have a repository
      And I have staged "points1"
      And I have staged "points2"
      And I have staged "lines1"
     When I run the command "commit -m Test"
     Then the response should contain "3 features added"
     
  Scenario: Try to perform multiple commits
    Given I have a repository
      And I have staged "points1"
      And I have staged "points2"
      And I have staged "lines1"
     When I run the command "commit -m Test"
     Then the response should contain "3 features added"
     When I modify and add a feature
      And I run the command "commit -m Test2"
     Then the response should contain "1 changed"
     
  Scenario: Try to commit without providing a message
    Given I have a repository
      And I have staged "points1"
      And I have staged "points2"
      And I have staged "lines1"
     When I run the command "commit"
     Then it should answer "No commit message provided"
     
  Scenario: Try to commit from an empty directory
    Given I am in an empty directory
     When I run the command "commit -m Test"
     Then the response should start with "Not a geogit repository"
     
  Scenario: Try to commit when no changes have been made
    Given I have a repository
     When I run the command "commit -m Test"
     Then the response should start with "Nothing to commit"

  Scenario: Try to commit when there is a merge conflict
    Given I have a repository
      And I have a merge conflict state
     When I run the command "commit -m Message"
     Then the response should contain "Cannot run operation while merge conflicts exist"
     
  Scenario: Try to commit without message while solving a merge conflict
    Given I have a repository
      And I have a merge conflict state
     When I run the command "checkout -p Points/Points.1 --theirs"
      And I run the command "add"
      And I run the command "commit"     
     Then the response should contain "Merge branch refs/heads/branch1"
      And the response should contain "Conflicts:"
      And the response should contain "Points/Points.1"             

     
  Scenario: Try to commit only points
    Given I have a repository
     And I have unstaged "points1"
     And I have unstaged "points2"
     And I have unstaged "lines1"
     And I have staged "lines2"
    When I run the command "commit -m Test Points"
    Then the response should contain "2 features added"
