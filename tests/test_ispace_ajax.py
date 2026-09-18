"""
Unit tests for iSpace AJAX Action Events Parser
"""

from services.ispace.client import IspaceClient

SAMPLE_ACTION_EVENTS = [
    {
        "error": False,
        "data": {
            "events": [
                {
                    "id": 116512,
                    "name": "Exercise 1 is due",
                    "activityname": "Exercise 1",
                    "timesort": 1789564200,
                    "url": "https://ispace.bnbu.edu.cn/mod/assign/view.php?id=354734",
                    "description": "<p>Take a screenshot...</p>",
                    "course": {
                        "id": 11233,
                        "fullname": "Data Analytics with Programming (Dr. Zhijian LI) [Semester 1 of 2026-2027]"
                    }
                },
                {
                    "id": 115982,
                    "name": "Assignment 1 is due",
                    "activityname": "Assignment 1",
                    "timesort": 1791388740,
                    "url": "https://ispace.bnbu.edu.cn/mod/assign/view.php?id=376275",
                    "description": "<p>Deadline Oct 7th</p>",
                    "course": {
                        "id": 11313,
                        "fullname": "Simulation Modelling (Prof. Ping HE) [Semester 1 of 2026-2027]"
                    }
                }
            ]
        }
    }
]

def test_parse_ispace_action_events():
    client = IspaceClient()
    ddls = client.parse_action_events_payload(SAMPLE_ACTION_EVENTS)
    assert len(ddls) == 2

    e1 = ddls[0]
    assert e1.id == "116512"
    assert e1.title == "Exercise 1"
    assert "Data Analytics" in e1.course_name
    assert e1.url == "https://ispace.bnbu.edu.cn/mod/assign/view.php?id=354734"

    e2 = ddls[1]
    assert e2.id == "115982"
    assert e2.title == "Assignment 1"
    assert "Simulation Modelling" in e2.course_name

def test_parse_course_tutorial():
    client = IspaceClient()
    sample_text = """
    Course information
    Instructor Information
    Prof. Xiaoling Peng
    Office: T3-501-R3

    Teaching Assistant Information
    Mr. Shengyang Xin
    Office: V26-202-52
    Email: xinshengyang@bnbu.edu.cn

    Mr. Qianzi Cao
    Office: T3-602-R25-H6

    Tutorial
    Time: Tuesday 18:00-19:50 From Week 3
    Location:  T3-201
    """
    tutorial_course = client.parse_course_tutorial(
        sample_text,
        course_name="Python for Data Science",
        course_code="DSBS7030"
    )
    assert tutorial_course is not None
    assert tutorial_course.name == "Python for Data Science (Tutorial)"
    assert tutorial_course.code == "DSBS7030"
    assert tutorial_course.classroom == "T3-201"
    assert tutorial_course.day_of_week == 2
    assert tutorial_course.start_period == 11
    assert tutorial_course.end_period == 12
    assert tutorial_course.start_week == 3
    assert tutorial_course.teacher == "Prof. Xiaoling PENG"
    assert "Shengyang Xin" in tutorial_course.assistant
