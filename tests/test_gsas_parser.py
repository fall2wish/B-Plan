"""
Unit tests for GSAS Timetable JSON Parser
"""

from services.gsas.client import GsasClient
from services.models import WeekType

SAMPLE_GSAS_TIMETABLE = {
    "status": 200,
    "result": {
        "studentNo": "2123456789",
        "cn": "张三",
        "en": "Zhang San",
        "programmeNameFull": "Master of Science in Data Science with Concentration in Business Statistics",
        "myTimetable": [
            {"timeId": 8, "dataList": [[], [], [], [], [], [], []]},
            {
                "timeId": 9,
                "dataList": [
                    [],
                    [
                        {
                            "id": "c1",
                            "ttsSection": {
                                "nameEn": "Python for Data Science",
                                "course": {"normal": "DSBS7030"},
                                "teachers": "Prof. Xiaoling PENG"
                            },
                            "place": "T4-305",
                            "week": "All"
                        }
                    ],
                    [], [],
                    [
                        {
                            "id": "c2",
                            "ttsSection": {
                                "nameEn": "Survey Sampling and Experimental Design",
                                "course": {"normal": "DSBS7130"},
                                "teachers": "Prof. Shan LI"
                            },
                            "place": "T5-401",
                            "week": "Odd"
                        }
                    ],
                    [], []
                ]
            },
            {
                "timeId": 10,
                "dataList": [
                    [],
                    [
                        {
                            "id": "c1",
                            "ttsSection": {
                                "nameEn": "Python for Data Science",
                                "course": {"normal": "DSBS7030"},
                                "teachers": "Prof. Xiaoling PENG"
                            },
                            "place": "T4-305",
                            "week": "All"
                        }
                    ],
                    [], [], [], [], []
                ]
            }
        ]
    }
}

def test_parse_gsas_timetable():
    client = GsasClient()
    courses = client.parse_timetable_payload(SAMPLE_GSAS_TIMETABLE)
    assert len(courses) == 2

    # Python for Data Science spans period 2 (timeId 9) to period 3 (timeId 10)
    py_course = next(c for c in courses if c.code == "DSBS7030")
    assert py_course.name == "Python for Data Science"
    assert py_course.day_of_week == 2  # Tuesday
    assert py_course.start_period == 2
    assert py_course.end_period == 3
    assert py_course.classroom == "T4-305"
    assert py_course.week_type == WeekType.ALL
    assert py_course.teacher == "Prof. Xiaoling PENG"

    # Survey Sampling (Odd week)
    survey_course = next(c for c in courses if c.code == "DSBS7130")
    assert survey_course.day_of_week == 5  # Friday
    assert survey_course.week_type == WeekType.ODD
    assert survey_course.classroom == "T5-401"


def test_parse_gsas_timetable_split_across_lunch():
    client = GsasClient()
    split_payload = {
        "status": 200,
        "result": {
            "myTimetable": [
                {
                    "timeId": 10,
                    "dataList": [
                        [{"id": "c1", "ttsSection": {"nameEn": "AI", "course": {"normal": "AI101"}}, "place": "T1-101", "week": "All"}],
                        [], [], [], [], [], []
                    ]
                },
                {
                    "timeId": 11,
                    "dataList": [
                        [{"id": "c1", "ttsSection": {"nameEn": "AI", "course": {"normal": "AI101"}}, "place": "T1-101", "week": "All"}],
                        [], [], [], [], [], []
                    ]
                },
                {
                    "timeId": 14,
                    "dataList": [
                        [{"id": "c1", "ttsSection": {"nameEn": "AI", "course": {"normal": "AI101"}}, "place": "T1-101", "week": "All"}],
                        [], [], [], [], [], []
                    ]
                },
                {
                    "timeId": 15,
                    "dataList": [
                        [{"id": "c1", "ttsSection": {"nameEn": "AI", "course": {"normal": "AI101"}}, "place": "T1-101", "week": "All"}],
                        [], [], [], [], [], []
                    ]
                }
            ]
        }
    }
    courses = client.parse_timetable_payload(split_payload)
    assert len(courses) == 2
    morn = courses[0]
    aft = courses[1]
    assert morn.start_period == 3
    assert morn.end_period == 4
    assert aft.start_period == 7
    assert aft.end_period == 8

